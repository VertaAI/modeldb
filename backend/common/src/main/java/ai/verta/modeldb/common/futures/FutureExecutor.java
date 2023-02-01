package ai.verta.modeldb.common.futures;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.Meter;
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
  private static volatile LongUpDownCounter activeFutureCounter;
  private final Executor delegate;
  private final String name;
  @With private final io.opentelemetry.context.Context otelContext;
  @With private final io.grpc.Context grpcContext;
  private final Attributes metricAttributes;

  public FutureExecutor(
      Executor delegate, String name, Context otelContext, io.grpc.Context grpcContext) {
    this.delegate = delegate;
    this.name = name;
    this.otelContext = otelContext;
    this.grpcContext = grpcContext;
    metricAttributes = Attributes.of(AttributeKey.stringKey("executor_name"), this.name);
  }

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
    Meter meter = openTelemetry.getMeter("verta.future");
    schedulingDelayHistogram =
        meter
            .histogramBuilder("future_exec_delay")
            .setDescription("The number of microseconds between creating and executing a Future")
            .ofLongs()
            .build();

    activeFutureCounter =
        meter
            .upDownCounterBuilder("active_futures")
            .setDescription("The number of currently active Futures")
            .build();
  }

  /**
   * Wraps an Executor and make it compatible with grpc's context
   *
   * @deprecated Please use {@link #makeCompatibleExecutor(Executor, String)} to provide better
   *     metrics.
   */
  @Deprecated(forRemoval = true)
  public static FutureExecutor makeCompatibleExecutor(Executor ex) {
    return makeCompatibleExecutor(ex, "unknown");
  }

  /** Wraps an Executor and make it compatible with grpc's context */
  public static FutureExecutor makeCompatibleExecutor(Executor ex, String name) {
    return new FutureExecutor(ex, name, null, null);
  }

  /**
   * @deprecated Please use {@link #initializeExecutor(Integer, String)} to provide better metrics.
   */
  @Deprecated(forRemoval = true)
  public static FutureExecutor initializeExecutor(Integer threadCount) {
    return initializeExecutor(threadCount, "unknown");
  }

  public static FutureExecutor initializeExecutor(Integer threadCount, String name) {
    return makeCompatibleExecutor(
        new ForkJoinPool(
            threadCount,
            ForkJoinPool.defaultForkJoinWorkerThreadFactory,
            Thread.getDefaultUncaughtExceptionHandler(),
            true),
        name);
  }

  public static FutureExecutor newSingleThreadExecutor() {
    return makeCompatibleExecutor(Executors.newSingleThreadExecutor(), "unknown");
  }

  private Runnable wrapWithTimer(Runnable r) {
    return () -> {
      if (schedulingDelayHistogram != null) {
        // pull the start time out of the context, and record how much time has elapsed.
        Long startTime = io.opentelemetry.context.Context.current().get(EXECUTION_TIMER_KEY);
        if (startTime != null) {
          long endTime = System.nanoTime();
          long microsDelay = (endTime - startTime) / 1_000L;
          schedulingDelayHistogram.record(microsDelay, metricAttributes);
        }
      }
      try {
        r.run();
      } finally {
        if (activeFutureCounter != null) {
          activeFutureCounter.add(-1, metricAttributes);
        }
      }
    };
  }

  @Override
  public void execute(@NonNull Runnable r) {
    if (activeFutureCounter != null) {
      activeFutureCounter.add(1, metricAttributes);
    }
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

  public String getName() {
    return name;
  }
}
