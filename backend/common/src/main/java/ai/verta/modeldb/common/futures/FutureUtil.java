package ai.verta.modeldb.common.futures;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.CompletableFuture;
import org.springframework.lang.NonNull;

public final class FutureUtil {
  private FutureUtil() {}

  /**
   * Converts a ListenableFuture, returned by a non-blocking call via grpc, to our custom
   * InternalFuture.
   */
  public static <T> InternalFuture<T> clientRequest(ListenableFuture<T> f, FutureExecutor ex) {
    CompletableFuture<T> promise = new CompletableFuture<>();
    Futures.addCallback(f, new Callback<T>(promise), ex);
    return InternalFuture.from(promise);
  }

  // Callback for a ListenableFuture to satisfy a promise
  static class Callback<T> implements FutureCallback<T> {
    final CompletableFuture<T> promise;
    final Exception callSiteHolder;

    Callback(CompletableFuture<T> promise) {
      this.promise = promise;
      this.callSiteHolder = new Exception("call site:");
    }

    @Override
    public void onSuccess(T t) {
      promise.complete(t);
    }

    @Override
    public void onFailure(@NonNull Throwable t) {
      t.addSuppressed(callSiteHolder);
      promise.completeExceptionally(t);
    }
  }
}
