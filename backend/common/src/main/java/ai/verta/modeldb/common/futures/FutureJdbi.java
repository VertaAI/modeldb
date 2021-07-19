package ai.verta.modeldb.common.futures;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.jdbi.v3.core.HandleCallback;
import org.jdbi.v3.core.HandleConsumer;
import org.jdbi.v3.core.Jdbi;

public class FutureJdbi {
  private final Executor executor;
  private final Jdbi jdbi;

  public FutureJdbi(Jdbi jdbi, Executor executor) {
    this.executor = executor;
    this.jdbi = jdbi;
  }

  public <R, T extends Exception> InternalFuture<R> withHandle(HandleCallback<R, T> callback) {
    CompletableFuture<R> promise = new CompletableFuture<R>();

    executor.execute(
        () -> {
          try {
            promise.complete(jdbi.withHandle(callback));
          } catch (Throwable e) {
            promise.completeExceptionally(e);
          }
        });

    return InternalFuture.from(promise);
  }

  public <R, T extends Exception> InternalFuture<R> withHandleCompose(
      HandleCallback<InternalFuture<R>, T> callback) {
    return withHandle(callback).thenCompose(x -> x, this.executor);
  }

  public <T extends Exception> InternalFuture<Void> useHandle(final HandleConsumer<T> consumer) {
    CompletableFuture<Void> promise = new CompletableFuture<Void>();

    executor.execute(
        () -> {
          try {
            jdbi.useHandle(consumer);
            promise.complete(null);
          } catch (Throwable e) {
            promise.completeExceptionally(e);
          }
        });

    return InternalFuture.from(promise);
  }
}
