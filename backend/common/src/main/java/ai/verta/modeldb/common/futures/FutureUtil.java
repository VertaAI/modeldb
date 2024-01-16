package ai.verta.modeldb.common.futures;

import com.google.common.util.concurrent.FutureCallback;
import java.util.concurrent.CompletableFuture;
import org.springframework.lang.NonNull;

public final class FutureUtil {
  private FutureUtil() {}

  // Callback for a ListenableFuture to satisfy a promise
  public static class Callback<T> implements FutureCallback<T> {
    final CompletableFuture<T> promise;

    public Callback(CompletableFuture<T> promise) {
      this.promise = promise;
    }

    @Override
    public void onSuccess(T t) {
      promise.complete(t);
    }

    @Override
    public void onFailure(@NonNull Throwable t) {
      promise.completeExceptionally(t);
    }
  }
}
