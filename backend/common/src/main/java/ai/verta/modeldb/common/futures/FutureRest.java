package ai.verta.modeldb.common.futures;

import ai.verta.modeldb.common.CommonUtils;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.GeneratedMessageV3;
import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import io.opentracing.util.GlobalTracer;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

@SuppressWarnings({"squid:S100"})
public class FutureRest {
  private FutureRest() {}

  // Injects the result of the future into the grpc StreamObserver as the return of the server
  public static <T> ResponseBodyEmitter ServerResponse(InternalFuture<T> f, Executor ex) {
    ResponseBodyEmitter observer = new ResponseBodyEmitter();
    f.whenComplete(
        (v, t) -> {
          if (t == null) {
            try {
              observer.send(v);
              observer.complete();
            } catch (IOException e) {
              CommonUtils.observeError(observer, t);
            }
          } else {
            CommonUtils.observeError(observer, t);
          }
        },
        ex);
    return observer;
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
