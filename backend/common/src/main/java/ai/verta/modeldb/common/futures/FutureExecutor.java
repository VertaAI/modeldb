package ai.verta.modeldb.common.futures;

import io.opentelemetry.context.Context;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import org.springframework.lang.NonNull;

public class FutureExecutor implements Executor {
  private final Executor delegate;

  private FutureExecutor(Executor delegate) {
    this.delegate = Context.taskWrapping(delegate);
  }

  // Wraps an Executor and make it compatible with grpc's context
  public static FutureExecutor makeCompatibleExecutor(Executor delegate) {
    return new FutureExecutor(delegate);
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
  public void execute(@NonNull Runnable runnable) {
    delegate.execute(runnable);
  }
}
