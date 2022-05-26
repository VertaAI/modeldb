package ai.verta.modeldb.common.futures;

import ai.verta.modeldb.common.CommonUtils;
import io.grpc.Context;
import io.opentracing.util.GlobalTracer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import org.springframework.lang.NonNull;

@SuppressWarnings({"squid:S100"})
public class FutureRest {
  private FutureRest() {}

  // Injects the result of the future into the grpc StreamObserver as the return of the server
  public static <T> CompletableFuture<T> serverResponse(InternalFuture<T> f, Executor ex) {
    CompletableFuture<T> promise = new CompletableFuture<>();
    f.whenComplete(
        (v, t) -> {
          if (t == null) {
            try {
              promise.complete(v);
            } catch (Throwable e) {
              CommonUtils.observeError(promise, t);
            }
          } else {
            CommonUtils.observeError(promise, t);
          }
        },
        ex);
    return promise;
  }

  // Wraps an Executor and make it compatible with grpc's context
  private static Executor makeCompatibleExecutor(Executor ex) {
    return new ExecutorWrapper(ex);
  }

  public static Executor initializeExecutor(Integer threadCount) {
    return FutureRest.makeCompatibleExecutor(
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
                      tracer.scopeManager().activate(span);
                      r.run();
                    }));
      } else {
        other.execute(Context.current().wrap(r));
      }
    }
  }
}
