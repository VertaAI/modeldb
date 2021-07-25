package ai.verta.modeldb.common.futures;

import org.jdbi.v3.core.HandleCallback;
import org.jdbi.v3.core.HandleConsumer;
import org.jdbi.v3.core.Jdbi;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class FutureJdbi {
  private final Executor executor;
  private final Jdbi jdbi;

  public FutureJdbi(Jdbi jdbi, Executor executor) {
    this.executor = executor;
    this.jdbi = jdbi;
  }

  public <R, T extends Exception> InternalFuture<R> withHandle(HandleCallback<R, T> callback) {
    return InternalFuture.trace(
        () -> {
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
        },
        "jdbi.withHandle",
        Map.of("caller", String.format("%s:%d", Thread.currentThread().getStackTrace()[2].getFileName(), Thread.currentThread().getStackTrace()[2].getLineNumber())),
        executor);
  }

  public <R, T extends Exception> InternalFuture<R> withHandleCompose(
      HandleCallback<InternalFuture<R>, T> callback) {
    return withHandle(callback).thenCompose(x -> x, this.executor);
  }

  public <T extends Exception> InternalFuture<Void> useHandle(final HandleConsumer<T> consumer) {
    return InternalFuture.trace(
        () -> {
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
        },
        "jdbi.useHandle",
            Map.of("caller", String.format("%s:%d", Thread.currentThread().getStackTrace()[2].getFileName(), Thread.currentThread().getStackTrace()[2].getLineNumber())),
        executor);
  }
}
