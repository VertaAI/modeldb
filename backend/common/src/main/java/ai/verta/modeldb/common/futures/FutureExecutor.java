package ai.verta.modeldb.common.futures;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.With;
import org.springframework.lang.NonNull;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class FutureExecutor implements Executor {
  private static final ContextKey<Long> EXECUTION_TIMER_KEY = ContextKey.named("execution timer");
  private static volatile LongHistogram schedulingDelayHistogram;
  private final Executor delegate;
  @With private final io.opentelemetry.context.Context otelContext;
  @With private final io.grpc.Context grpcContext;

  public FutureExecutor captureContext() {
    io.opentelemetry.context.Context otelContext = Context.current();
    return withOtelContext(otelContext).withGrpcContext(io.grpc.Context.current());
  }

  /**
   * Allow the FutureExecutors to have instrumentation via OpenTelemetry. Set this as early as
   * possible after creation, in order not to miss any metrics that might be recorded.
   */
  public static void setOpenTelemetry(OpenTelemetry openTelemetry) {
    // it's ok to do this more than once, since creation of meters/instruments is idempotent in the
    // OTel SDK.
    schedulingDelayHistogram =
        openTelemetry
            .getMeter("verta.future")
            .histogramBuilder("future_exec_delay")
            .setDescription("The number of microseconds between creating and executing a Future")
            .ofLongs()
            .build();
  }

  // Wraps an Executor and make it compatible with grpc's context
  public static FutureExecutor makeCompatibleExecutor(Executor ex) {
    return new FutureExecutor(Context.taskWrapping(ex), null, null);
  }

  public static FutureExecutor initializeExecutor(Integer threadCount) {
    return makeCompatibleExecutor(
        new ForkJoinPool(
            threadCount,
            ForkJoinPool.defaultForkJoinWorkerThreadFactory,
            Thread.getDefaultUncaughtExceptionHandler(),
            true));
  }

  public static FutureExecutor newSingleThreadExecutor() {
    return makeCompatibleExecutor(Executors.newSingleThreadExecutor());
  }

  private Runnable wrapWithTimer(Runnable r) {
    return () -> {
      if (schedulingDelayHistogram != null) {
        // pull the start time out of the context, and record how much time has elapsed.
        Long startTime = io.opentelemetry.context.Context.current().get(EXECUTION_TIMER_KEY);
        if (startTime != null) {
          long endTime = System.nanoTime();
          long microsDelay = (endTime - startTime) / 1_000L;
          schedulingDelayHistogram.record(microsDelay);
        }
      }
      r.run();
    };
  }

  @Override
  public void execute(@NonNull Runnable r) {
    r = wrapWithTimer(r);

    if (otelContext != null) {
      // set the current nano time into the context, so we can measure how long it takes to actually
      // start the future execution after submission to the executor.
      r = otelContext.with(EXECUTION_TIMER_KEY, System.nanoTime()).wrap(r);
    }
    if (grpcContext != null) {
      r = grpcContext.wrap(r);
    }

    delegate.execute(r);
  }
}
