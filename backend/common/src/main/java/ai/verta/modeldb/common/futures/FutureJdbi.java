package ai.verta.modeldb.common.futures;

import java.util.concurrent.CompletableFuture;
import org.jdbi.v3.core.statement.StatementExceptions;

public class FutureJdbi {
  private final FutureExecutor executor;
  private final InternalJdbi jdbi;

  public FutureJdbi(InternalJdbi jdbi, FutureExecutor executor) {
    this.executor = executor;
    this.jdbi = jdbi;
    // Ensure that we do not log any sensitive/private data when exceptions are logged
    this.jdbi
        .getConfig(StatementExceptions.class)
        .setMessageRendering(StatementExceptions.MessageRendering.NONE);
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

  public <R, T extends Exception> InternalFuture<R> withTransaction(HandleCallback<R, T> callback) {
    SupplierWithException<R, T> supplierWithException = () -> jdbi.inTransaction(callback);
    return withHandleOrTransaction(supplierWithException);
  }

  private <R, T extends Exception> InternalFuture<R> withHandleOrTransaction(
      SupplierWithException<R, T> supplier) {
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
    RunnableWithException<T> runnableWithException = () -> jdbi.useTransaction(consumer);
    return useHandleOrTransaction(runnableWithException);
  }

  private <T extends Exception> InternalFuture<Void> useHandleOrTransaction(
      final RunnableWithException<T> runnableWithException) {
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
  }

  public <R, T extends Exception> Future<R> handleCompose(HandleCallback<Future<R>, T> callback) {
    return handle(callback).thenCompose(x -> x);
  }

  public <R, T extends Exception> Future<R> handle(HandleCallback<R, T> callback) {
    SupplierWithException<R, T> supplierWithException = () -> jdbi.withHandle(callback);
    return handleOrTransaction(supplierWithException);
  }

  public <R, T extends Exception> Future<R> inTransaction(HandleCallback<R, T> callback) {
    SupplierWithException<R, T> supplierWithException = () -> jdbi.inTransaction(callback);
    return handleOrTransaction(supplierWithException);
  }

  private <R, T extends Exception> Future<R> handleOrTransaction(
      SupplierWithException<R, T> supplier) {
    CompletableFuture<R> promise = new CompletableFuture<>();

    executor.execute(
        () -> {
          try {
            promise.complete(supplier.get());
          } catch (Throwable e) {
            promise.completeExceptionally(e);
          }
        });

    return Future.from(promise);
  }

  public <T extends Exception> Future<Void> handle(HandleConsumer<T> consumer) {
    RunnableWithException<T> runnableWithException = () -> jdbi.useHandle(consumer);
    return handleOrTransaction(runnableWithException);
  }

  public <T extends Exception> Future<Void> transaction(HandleConsumer<T> consumer) {
    RunnableWithException<T> runnableWithException = () -> jdbi.useTransaction(consumer);
    return handleOrTransaction(runnableWithException);
  }

  private <T extends Exception> Future<Void> handleOrTransaction(
      RunnableWithException<T> runnableWithException) {
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

    return Future.from(promise);
  }
}
