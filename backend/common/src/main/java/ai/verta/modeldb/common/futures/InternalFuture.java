package ai.verta.modeldb.common.futures;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class InternalFuture<T> {
  private CompletionStage<T> stage;

  private InternalFuture() {}

  public static <T> InternalFuture<List<T>> sequence(final List<InternalFuture<T>> futures) {
    var completableFuture =
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
                    .thenApply(
                            unit ->
                                    futures.stream()
                                            .map(f -> f.stage.toCompletableFuture().join())
                                            .collect(Collectors.toList()));
    return InternalFuture.from(completableFuture);
  }

  public static <R> InternalFuture<R> from(CompletionStage<R> other) {
    var ret = new InternalFuture<R>();
    ret.stage = other;
    return ret;
  }

  public static <R> InternalFuture<R> completedInternalFuture(R value) {
    var ret = new InternalFuture<R>();
    ret.stage = CompletableFuture.completedFuture(value);
    return ret;
  }

  public <U> InternalFuture<U> thenCompose(
      Function<? super T, InternalFuture<U>> fn, Executor executor) {
    return from(
        stage.thenComposeAsync(fn.andThen(internalFuture -> internalFuture.stage), executor));
  }

  public <U> InternalFuture<U> thenApply(Function<? super T, ? extends U> fn, Executor executor) {
    return from(stage.thenApplyAsync(fn, executor));
  }

  public InternalFuture<Void> thenAccept(Consumer<? super T> action, Executor executor) {
    return from(stage.thenAcceptAsync(action, executor));
  }

  public <U> InternalFuture<Void> thenRun(Runnable runnable, Executor executor) {
    return from(stage.thenRunAsync(runnable, executor));
  }

  public static <U> InternalFuture<Void> runAsync(Runnable runnable, Executor executor) {
    return from(CompletableFuture.runAsync(runnable, executor));
  }

  public static <U> InternalFuture<U> supplyAsync(Supplier<U> supplier, Executor executor) {
    return from(CompletableFuture.supplyAsync(supplier, executor));
  }

  public static <U> InternalFuture<U> failedStage(Throwable ex) {
    return from(CompletableFuture.failedFuture(ex));
  }

  public InternalFuture<T> whenComplete(
      BiConsumer<? super T, ? super Throwable> action, Executor executor) {
    return from(stage.whenCompleteAsync(action, executor));
  }

  public T get() throws ExecutionException, InterruptedException {
    return stage.toCompletableFuture().get();
  }

  public CompletionStage<T> toCompletionStage() {
    return stage;
  }
}
