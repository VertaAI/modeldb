package ai.verta.modeldb.common.futures;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.With;
import org.springframework.lang.NonNull;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class FutureExecutor implements Executor {
  private final Executor delegate;
  @With private final io.opentelemetry.context.Context otelContext;
  @With private final io.grpc.Context grpcContext;

  public FutureExecutor captureContext() {
    return withOtelContext(io.opentelemetry.context.Context.current())
        .withGrpcContext(io.grpc.Context.current());
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

  @Override
  public void execute(@NonNull Runnable r) {
    if (otelContext != null) {
      r = otelContext.wrap(r);
    }
    if (grpcContext != null) {
      r = grpcContext.wrap(r);
    }

    other.execute(r);
  }
}
