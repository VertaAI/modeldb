package ai.verta.modeldb.common.futures;

import io.opentelemetry.api.GlobalOpenTelemetry;
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
    // set the current nano time into the context, so we can measure how long it takes to actually
    // execute the future.
    io.opentelemetry.context.Context otelContext = Context.current();
    otelContext = otelContext.with(EXECUTION_TIMER_KEY, System.nanoTime());
    return withOtelContext(otelContext).withGrpcContext(io.grpc.Context.current());
  }

  private static LongHistogram getSchedulingDelayHistogram() {
    if (schedulingDelayHistogram != null) {
      return schedulingDelayHistogram;
    }
    schedulingDelayHistogram =
        GlobalOpenTelemetry.get()
            .getMeter("verta.future")
            .histogramBuilder("future_exec_delay")
            .setDescription("The number of microseconds between creating and executing a Future")
            .ofLongs()
            .build();
    return schedulingDelayHistogram;
  }

  // Wraps an Executor and make it compatible with grpc's context
  public static FutureExecutor makeCompatibleExecutor(Executor ex) {
    return new FutureExecutor(ex, null, null);
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
      // pull the start time out of the context, and record how much time has elapsed.
      Long startTime = io.opentelemetry.context.Context.current().get(EXECUTION_TIMER_KEY);
      if (startTime != null) {
        long endTime = System.nanoTime();
        long microsDelay = (endTime - startTime) / 1_000L;
        getSchedulingDelayHistogram().record(microsDelay);
      }
      r.run();
    };
  }

  @Override
  public void execute(@NonNull Runnable r) {
    r = wrapWithTimer(r);

    if (otelContext != null) {
      r = otelContext.wrap(r);
    }
    if (grpcContext != null) {
      r = grpcContext.wrap(r);
    }

    delegate.execute(r);
  }
}
