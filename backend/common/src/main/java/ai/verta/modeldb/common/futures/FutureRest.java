package ai.verta.modeldb.common.futures;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

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
