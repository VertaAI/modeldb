package ai.verta.modeldb.common.futures;

import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.Context;
import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import org.springframework.lang.NonNull;

public final class FutureUtil {
  private FutureUtil() {}

  // Wraps an Executor and make it compatible with grpc's context
  public static Executor makeCompatibleExecutor(Executor ex) {
    return new ExecutorWrapper(ex);
  }

  public static Executor initializeExecutor(Integer threadCount) {
    return makeCompatibleExecutor(
        new ForkJoinPool(
            threadCount,
            ForkJoinPool.defaultForkJoinWorkerThreadFactory,
            Thread.getDefaultUncaughtExceptionHandler(),
            true));
  }

  private static class ExecutorWrapper implements Executor {
    final Executor other;

    ExecutorWrapper(Executor other) {
      this.other = other;
    }

    @Override
    public void execute(@NonNull Runnable r) {
      if (GlobalTracer.isRegistered()) {
        final var tracer = GlobalTracer.get();
        final var span = tracer.scopeManager().activeSpan();
        other.execute(
            Context.current()
                .wrap(
                    () -> {
                      try (Scope s = tracer.scopeManager().activate(span)) {
                        r.run();
                      }
                    }));
      } else {
        other.execute(Context.current().wrap(r));
      }
    }
  }

  // Converts a ListenableFuture, returned by a non-blocking call via grpc, to our custom
  // InternalFuture
  public static <T> InternalFuture<T> clientRequest(ListenableFuture<T> f, Executor ex) {
    return InternalFuture.from(f, ex);
  }
}
