package ai.verta.modeldb.common.futures;

import ai.verta.modeldb.common.CommonUtils;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class FutureRest {
  private FutureRest() {}

  // Injects the result of the future into the grpc StreamObserver as the return of the server
  public static <T> CompletableFuture<T> serverResponse(InternalFuture<T> future, FutureExecutor ex) {
    CompletableFuture<T> promise = new CompletableFuture<>();
    future.whenComplete(
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
}
