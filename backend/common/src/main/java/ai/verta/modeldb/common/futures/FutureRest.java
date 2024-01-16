package ai.verta.modeldb.common.futures;

import ai.verta.modeldb.common.CommonUtils;
import java.util.concurrent.CompletableFuture;

public class FutureRest {
  public static <T> CompletableFuture<T> serverResponse(Future<T> future) {
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
        });
    return promise;
  }
}
