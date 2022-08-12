package ai.verta.modeldb.common.futures;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class InternalFutureWithDefaultExecutor<T> extends InternalFuture<T> {

  private Executor defaultExecutor = null;

  InternalFutureWithDefaultExecutor(CompletionStage<T> stage) {
    super(stage);
  }

  @Override
  public InternalFutureWithDefaultExecutor<T> thenWithExecutor(Executor executor) {
    defaultExecutor = executor;
    return this;
  }

  public <U> InternalFutureWithDefaultExecutor<U> thenCompose(
      Function<? super T, InternalFuture<U>> fn) {
    Objects.requireNonNull(
        defaultExecutor, "Cached executor required to use this method signature");
    return thenCompose(fn, defaultExecutor);
  }

  @Override
  @Deprecated
  public <U> InternalFutureWithDefaultExecutor<U> thenCompose(
      Function<? super T, InternalFuture<U>> fn, Executor executor) {
    return super.thenCompose(fn, executor).thenWithExecutor(defaultExecutor);
  }

  public <U> InternalFutureWithDefaultExecutor<U> thenApply(Function<? super T, ? extends U> fn) {
    Objects.requireNonNull(
        defaultExecutor, "Cached executor required to use this method signature");
    return thenApply(fn, defaultExecutor);
  }

  @Override
  @Deprecated
  public <U> InternalFutureWithDefaultExecutor<U> thenApply(
      Function<? super T, ? extends U> fn, Executor executor) {
    return (InternalFutureWithDefaultExecutor<U>)
        super.thenApply(fn, executor).thenWithExecutor(defaultExecutor);
  }

  public <U> InternalFutureWithDefaultExecutor<U> handle(
      BiFunction<? super T, Throwable, ? extends U> fn) {
    Objects.requireNonNull(
        defaultExecutor, "Cached executor required to use this method signature");
    return handle(fn, defaultExecutor);
  }

  @Override
  @Deprecated
  public <U> InternalFutureWithDefaultExecutor<U> handle(
      BiFunction<? super T, Throwable, ? extends U> fn, Executor executor) {
    return (InternalFutureWithDefaultExecutor<U>)
        super.handle(fn, executor).thenWithExecutor(defaultExecutor);
  }

  public <U, V> InternalFutureWithDefaultExecutor<V> thenCombine(
      InternalFuture<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn) {
    Objects.requireNonNull(
        defaultExecutor, "Cached executor required to use this method signature");
    return thenCombine(other, fn, defaultExecutor);
  }

  @Override
  @Deprecated
  public <U, V> InternalFutureWithDefaultExecutor<V> thenCombine(
      InternalFuture<? extends U> other,
      BiFunction<? super T, ? super U, ? extends V> fn,
      Executor executor) {
    return (InternalFutureWithDefaultExecutor<V>)
        super.thenCombine(other, fn, executor).thenWithExecutor(defaultExecutor);
  }

  public InternalFutureWithDefaultExecutor<Void> thenAccept(Consumer<? super T> action) {
    Objects.requireNonNull(
        defaultExecutor, "Cached executor required to use this method signature");
    return thenAccept(action, defaultExecutor);
  }

  @Override
  @Deprecated
  public InternalFutureWithDefaultExecutor<Void> thenAccept(
      Consumer<? super T> action, Executor executor) {
    return super.thenAccept(action, executor).thenWithExecutor(defaultExecutor);
  }

  public <U> InternalFutureWithDefaultExecutor<Void> thenRun(Runnable runnable) {
    Objects.requireNonNull(
        defaultExecutor, "Cached executor required to use this method signature");
    return thenRun(runnable, defaultExecutor);
  }

  @Override
  @Deprecated
  public <U> InternalFutureWithDefaultExecutor<Void> thenRun(Runnable runnable, Executor executor) {
    return super.thenRun(runnable, executor).thenWithExecutor(defaultExecutor);
  }

  public InternalFutureWithDefaultExecutor<T> whenComplete(
      BiConsumer<? super T, ? super Throwable> action) {
    Objects.requireNonNull(
        defaultExecutor, "Cached executor required to use this method signature");
    return whenComplete(action, defaultExecutor);
  }

  @Override
  @Deprecated
  public InternalFutureWithDefaultExecutor<T> whenComplete(
      BiConsumer<? super T, ? super Throwable> action, Executor executor) {
    return super.whenComplete(action, executor).thenWithExecutor(defaultExecutor);
  }

  public <U> InternalFutureWithDefaultExecutor<U> thenSupply(Supplier<InternalFuture<U>> supplier) {
    Objects.requireNonNull(
        defaultExecutor, "Cached executor required to use this method signature");
    return thenSupply(supplier, defaultExecutor);
  }

  @Override
  @Deprecated
  public <U> InternalFutureWithDefaultExecutor<U> thenSupply(
      Supplier<InternalFuture<U>> supplier, Executor executor) {
    return super.thenSupply(supplier, executor).thenWithExecutor(defaultExecutor);
  }

  public static class FactoryWithExecutor {

    private Executor executor;

    private FactoryWithExecutor(Executor executor) {
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
}
