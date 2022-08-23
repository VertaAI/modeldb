package ai.verta.modeldb.common.futures;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

public class InternalFutureWithDefaultExecutorFactory {

  private Executor executor;

  InternalFutureWithDefaultExecutorFactory(Executor executor) {
    Objects.requireNonNull(executor, "Valid executor required");
    this.executor = executor;
  }

  public <U> InternalFutureWithDefaultExecutor<U> completedInternalFuture(U value) {
    return InternalFuture.completedInternalFuture(value).thenWithExecutor(executor);
  }

  public <U> InternalFutureWithDefaultExecutor<U> from(CompletionStage<U> stage) {
    return InternalFuture.from(stage).thenWithExecutor(executor);
  }

  public <U> InternalFutureWithDefaultExecutor<U> from(ListenableFuture<U> listenableFuture) {
    return InternalFuture.from(listenableFuture, executor).thenWithExecutor(executor);
  }

  public InternalFutureWithDefaultExecutor<Void> runAsync(Runnable runnable) {
    return InternalFuture.runAsync(runnable, executor).thenWithExecutor(executor);
  }

  public <U> InternalFutureWithDefaultExecutor<List<U>> sequence(
      final List<InternalFuture<U>> futures) {
    return InternalFuture.sequence(futures, executor).thenWithExecutor(executor);
  }

  public <U> InternalFutureWithDefaultExecutor<U> supplyAsync(Supplier<U> supplier) {
    return InternalFuture.supplyAsync(supplier, executor).thenWithExecutor(executor);
  }

  public <U> InternalFuture<U> failedStage(Throwable ex) {
    return InternalFuture.<U>failedStage(ex).thenWithExecutor(executor);
  }

  public <U> InternalFutureWithDefaultExecutor<U> retriableStage(
      Supplier<InternalFuture<U>> supplier, Function<Throwable, Boolean> retryChecker) {
    return InternalFuture.retriableStage(supplier, retryChecker, executor)
        .thenWithExecutor(executor);
  }
}
