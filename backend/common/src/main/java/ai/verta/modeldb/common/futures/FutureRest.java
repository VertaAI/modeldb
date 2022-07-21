package ai.verta.modeldb.common.futures;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class FutureRest {
  private FutureRest() {}

  // Injects the result of the future into the grpc StreamObserver as the return of the server
  public static <T> CompletableFuture<T> serverResponse(InternalFuture<T> future, Executor ex) {
    CompletableFuture<T> promise = new CompletableFuture<>();
    future.whenComplete(
        (v, t) -> {
          if (t == null) {
            try {
              promise.complete(v);
            } catch (Throwable e) {
              promise.completeExceptionally(e);
            }
          } else {
            promise.completeExceptionally(t);
          }
        },
        ex);
    return promise;
  }
}
