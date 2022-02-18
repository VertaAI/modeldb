package ai.verta.modeldb.common.futures;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.jdbi.v3.core.HandleCallback;
import org.jdbi.v3.core.HandleConsumer;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.StatementExceptions;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;

public class FutureJdbi {
  private final Executor executor;
  private final Jdbi jdbi;

  public FutureJdbi(Jdbi jdbi, Executor executor) {
    this.executor = executor;
    this.jdbi = jdbi;
      // Ensure that we do not log any sensitive/private data when exceptions are logged
      this.jdbi.getConfig(StatementExceptions.class).setMessageRendering(StatementExceptions.MessageRendering.NONE);
  }

  @FunctionalInterface
  private interface SupplierWithException<R, T extends Exception> {
    R get() throws T;
  }

  @FunctionalInterface
  private interface RunnableWithException<T extends Exception> {
    void run() throws T;
  }

  public <R, T extends Exception> InternalFuture<R> withHandle(HandleCallback<R, T> callback) {
    SupplierWithException<R, T> supplierWithException = () -> jdbi.withHandle(callback);
    return withHandleOrTransaction(supplierWithException);
  }

  public <R, T extends Exception> InternalFuture<R> withTransactionIsolationLevel(
      HandleCallback<R, T> callback, TransactionIsolationLevel transactionIsolationLevel) {
    SupplierWithException<R, T> supplierWithException =
        () -> jdbi.inTransaction(transactionIsolationLevel, callback);
    return withHandleOrTransaction(supplierWithException);
  }

  public <R, T extends Exception> InternalFuture<R> withTransaction(HandleCallback<R, T> callback) {
    return withTransactionIsolationLevel(callback, TransactionIsolationLevel.SERIALIZABLE);
  }

  private <R, T extends Exception> InternalFuture<R> withHandleOrTransaction(
      SupplierWithException<R, T> supplier) {
    return InternalFuture.trace(
        () -> {
          CompletableFuture<R> promise = new CompletableFuture<>();

          executor.execute(
              () -> {
                try {
                  promise.complete(supplier.get());
                } catch (Throwable e) {
                  promise.completeExceptionally(e);
                }
              });

          return InternalFuture.from(promise);
        },
        "jdbi.withHandle",
        Map.of(
            "caller",
            String.format(
                "%s:%d",
                Thread.currentThread().getStackTrace()[2].getFileName(),
                Thread.currentThread().getStackTrace()[2].getLineNumber())),
        executor);
  }

  public <R, T extends Exception> InternalFuture<R> withHandleCompose(
      HandleCallback<InternalFuture<R>, T> callback) {
    return withHandle(callback).thenCompose(x -> x, this.executor);
  }

  public <T extends Exception> InternalFuture<Void> useHandle(final HandleConsumer<T> consumer) {
    RunnableWithException<T> runnableWithException = () -> jdbi.useHandle(consumer);
    return useHandleOrTransaction(runnableWithException);
  }

  public <T extends Exception> InternalFuture<Void> useTransaction(
      final HandleConsumer<T> consumer) {
    return useTransactionIsolationLevel(consumer, TransactionIsolationLevel.SERIALIZABLE);
  }

  public <T extends Exception> InternalFuture<Void> useTransactionIsolationLevel(
      final HandleConsumer<T> consumer, TransactionIsolationLevel transactionIsolationLevel) {
    RunnableWithException<T> runnableWithException =
        () -> jdbi.useTransaction(transactionIsolationLevel, consumer);
    return useHandleOrTransaction(runnableWithException);
  }

  private <T extends Exception> InternalFuture<Void> useHandleOrTransaction(
      final RunnableWithException<T> runnableWithException) {
    return InternalFuture.trace(
        () -> {
          CompletableFuture<Void> promise = new CompletableFuture<>();

          executor.execute(
              () -> {
                try {
                  runnableWithException.run();
                  promise.complete(null);
                } catch (Throwable e) {
                  promise.completeExceptionally(e);
                }
              });

          return InternalFuture.from(promise);
        },
        "jdbi.useHandle",
        Map.of(
            "caller",
            String.format(
                "%s:%d",
                Thread.currentThread().getStackTrace()[2].getFileName(),
                Thread.currentThread().getStackTrace()[2].getLineNumber())),
        executor);
  }
}
