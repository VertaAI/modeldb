package ai.verta.modeldb.common.futures;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class InternalFutureWithDefaultExecutor<T> extends InternalFuture<T> {

  private final Executor defaultExecutor;

  InternalFutureWithDefaultExecutor(CompletionStage<T> stage, Executor executor) {
    super(stage);
    Objects.requireNonNull(executor, "Valid executor required");
    defaultExecutor = executor;
  }

  public <U> InternalFutureWithDefaultExecutor<U> thenCompose(
      Function<? super T, InternalFuture<U>> fn) {
    return thenCompose(fn, defaultExecutor);
  }

  @Override
  @Deprecated
  public <U> InternalFutureWithDefaultExecutor<U> thenCompose(
      Function<? super T, InternalFuture<U>> fn, Executor executor) {
    return super.thenCompose(fn, executor).thenWithExecutor(defaultExecutor);
  }

  public <U> InternalFutureWithDefaultExecutor<U> thenApply(Function<? super T, ? extends U> fn) {
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
    return thenAccept(action, defaultExecutor);
  }

  @Override
  @Deprecated
  public InternalFutureWithDefaultExecutor<Void> thenAccept(
      Consumer<? super T> action, Executor executor) {
    return super.thenAccept(action, executor).thenWithExecutor(defaultExecutor);
  }

  public <U> InternalFutureWithDefaultExecutor<Void> thenRun(Runnable runnable) {
    return thenRun(runnable, defaultExecutor);
  }

  @Override
  @Deprecated
  public <U> InternalFutureWithDefaultExecutor<Void> thenRun(Runnable runnable, Executor executor) {
    return super.thenRun(runnable, executor).thenWithExecutor(defaultExecutor);
  }

  public InternalFutureWithDefaultExecutor<T> whenComplete(
      BiConsumer<? super T, ? super Throwable> action) {
    return whenComplete(action, defaultExecutor);
  }

  @Override
  @Deprecated
  public InternalFutureWithDefaultExecutor<T> whenComplete(
      BiConsumer<? super T, ? super Throwable> action, Executor executor) {
    return super.whenComplete(action, executor).thenWithExecutor(defaultExecutor);
  }

  public <U> InternalFutureWithDefaultExecutor<U> thenSupply(Supplier<InternalFuture<U>> supplier) {
    return thenSupply(supplier, defaultExecutor);
  }

  @Override
  @Deprecated
  public <U> InternalFutureWithDefaultExecutor<U> thenSupply(
      Supplier<InternalFuture<U>> supplier, Executor executor) {
    return super.thenSupply(supplier, executor).thenWithExecutor(defaultExecutor);
  }
}
