package ai.verta.modeldb.common.futures;

import ai.verta.modeldb.common.exceptions.ModelDBException;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.*;
import java.util.stream.Collectors;

public class InternalFuture<T> {
  private static final boolean DEEP_TRACING_ENABLED;
  public static final AttributeKey<String> STACK_ATTRIBUTE_KEY = AttributeKey.stringKey("stack");

  static {
    String internalFutureTracingEnabled = System.getenv("IFUTURE_TRACING_ENABLED");
    DEEP_TRACING_ENABLED = Boolean.parseBoolean(internalFutureTracingEnabled);
  }

  private final Tracer futureTracer = GlobalOpenTelemetry.getTracer("futureTracer");
  private final String formattedStack;
  private final CompletionStage<T> stage;

  // keep the calling OpenTelemetry context, so we can use it to wrap the actual invocation of the
  // future's implementation
  private final Context callingContext = Context.current();

  private InternalFuture(final CompletionStage<T> stage) {
    this.stage = stage;
    if (!DEEP_TRACING_ENABLED) {
      formattedStack = null;
    } else {
      StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
      if (stackTrace.length > 10) {
        stackTrace = Arrays.copyOf(stackTrace, 10);
      }
      this.formattedStack =
          Arrays.stream(stackTrace)
              .map(StackTraceElement::toString)
              .collect(Collectors.joining("\nat "));
    }
  }

  // Convert a list of futures to a future of a list
  @SuppressWarnings("unchecked")
  public static <T> InternalFuture<List<T>> sequence(
      final List<InternalFuture<T>> futures, Executor executor) {
    if (futures.isEmpty()) {
      return InternalFuture.completedInternalFuture(new LinkedList<>());
    }

    final var promise = new CompletableFuture<List<T>>();
    final var values = new ArrayList<T>(futures.size());
    final CompletableFuture<T>[] futuresArray =
        futures.stream()
            .map(x -> x.toCompletionStage().toCompletableFuture())
            .collect(Collectors.toList())
            .toArray(new CompletableFuture[futures.size()]);

    CompletableFuture.allOf(futuresArray)
        .whenCompleteAsync(
            (ignored, throwable) -> {
              if (throwable != null) {
                promise.completeExceptionally(throwable);
                return;
              }

              try {
                for (final var future : futuresArray) {
                  values.add(future.get());
                }
                promise.complete(values);
              } catch (RuntimeException | InterruptedException | ExecutionException t) {
                if (t instanceof InterruptedException) {
                  // Restore interrupted state...
                  Thread.currentThread().interrupt();
                }
                promise.completeExceptionally(t);
              }
            },
            executor);

    return from(promise);
  }

  public static <R> InternalFuture<R> from(CompletionStage<R> other) {
    return new InternalFuture<>(other);
  }

  // Converts a ListenableFuture, returned by a non-blocking call via grpc, to our custom
  // InternalFuture
  public static <T> InternalFuture<T> from(ListenableFuture<T> f, Executor ex) {
    CompletableFuture<T> promise = new CompletableFuture<>();
    Futures.addCallback(f, new Callback<T>(promise), ex);
    return InternalFuture.from(promise);
  }

  public static <R> InternalFuture<R> completedInternalFuture(R value) {
    return from(CompletableFuture.completedFuture(value));
  }

  public static FactoryWithExecutor withExecutor(Executor executor) {
    return new FactoryWithExecutor(executor);
  }

  public WithCachedExecutor<T> thenWithExecutor(Executor executor) {
    return new WithCachedExecutor<>(this.stage).thenWithExecutor(executor);
  }

  public <U> InternalFuture<U> thenCompose(
      Function<? super T, InternalFuture<U>> fn, Executor executor) {
    return InternalFuture.<U>from(
        stage.thenComposeAsync(
            traceFunction(
                callingContext.wrapFunction(
                    traceFunction(
                        fn.andThen(internalFuture -> internalFuture.stage), "futureThenCompose")),
                "futureThenApply"),
            executor));
  }

  public <U> InternalFuture<U> thenApply(Function<? super T, ? extends U> fn, Executor executor) {
    return InternalFuture.<U>from(
        stage.thenApplyAsync(
            traceFunction(callingContext.wrapFunction(fn), "futureThenApply"), executor));
  }

  private <U> Function<? super T, ? extends U> traceFunction(
      Function<? super T, ? extends U> fn, String spanName) {
    if (!DEEP_TRACING_ENABLED) {
      return fn;
    }
    return t -> {
      Span span = startSpan(spanName);
      try (Scope ignored = span.makeCurrent()) {
        return fn.apply(t);
      } finally {
        span.end();
      }
    };
  }

  private Span startSpan(String futureThenApply) {
    return futureTracer
        .spanBuilder(futureThenApply)
        .setAttribute(STACK_ATTRIBUTE_KEY, formattedStack)
        .startSpan();
  }

  public <U> InternalFuture<U> handle(
      BiFunction<? super T, Throwable, ? extends U> fn, Executor executor) {
    return InternalFuture.<U>from(
        stage.handleAsync(traceBiFunctionThrowable(callingContext.wrapFunction(fn)), executor));
  }

  private <U> BiFunction<? super T, Throwable, ? extends U> traceBiFunctionThrowable(
      BiFunction<? super T, Throwable, ? extends U> fn) {
    if (!DEEP_TRACING_ENABLED) {
      return fn;
    }
    return (t, throwable) -> {
      Span span = startSpan("futureHandle");
      try (Scope ignored = span.makeCurrent()) {
        return fn.apply(t, throwable);
      } finally {
        span.end();
      }
    };
  }

  public <U, V> InternalFuture<V> thenCombine(
      InternalFuture<? extends U> other,
      BiFunction<? super T, ? super U, ? extends V> fn,
      Executor executor) {
    return InternalFuture.<V>from(
        stage.thenCombineAsync(
            other.stage, callingContext.wrapFunction(traceBiFunction(fn)), executor));
  }

  private <U, V> BiFunction<? super T, ? super U, ? extends V> traceBiFunction(
      BiFunction<? super T, ? super U, ? extends V> fn) {
    if (!DEEP_TRACING_ENABLED) {
      return fn;
    }
    return (t, u) -> {
      Span span = startSpan("futureThenCombine");
      try (Scope ignored = span.makeCurrent()) {
        return fn.apply(t, u);
      } finally {
        span.end();
      }
    };
  }

  public InternalFuture<Void> thenAccept(Consumer<? super T> action, Executor executor) {
    return from(
        stage.thenAcceptAsync(callingContext.wrapConsumer(traceConsumer(action)), executor));
  }

  private Consumer<? super T> traceConsumer(Consumer<? super T> action) {
    if (!DEEP_TRACING_ENABLED) {
      return action;
    }
    return t -> {
      Span span = startSpan("futureThenAccept");
      try (Scope ignored = span.makeCurrent()) {
        action.accept(t);
      } finally {
        span.end();
      }
    };
  }

  public <U> InternalFuture<Void> thenRun(Runnable runnable, Executor executor) {
    return from(stage.thenRunAsync(callingContext.wrap(traceRunnable(runnable)), executor));
  }

  private Runnable traceRunnable(Runnable r) {
    if (!DEEP_TRACING_ENABLED) {
      return r;
    }
    return () -> {
      Span span = startSpan("futureThenRun");
      try (Scope ignored = span.makeCurrent()) {
        r.run();
      } finally {
        span.end();
      }
    };
  }

  public InternalFuture<T> whenComplete(
      BiConsumer<? super T, ? super Throwable> action, Executor executor) {
    return from(
        stage.whenCompleteAsync(callingContext.wrapConsumer(traceBiConsumer(action)), executor));
  }

  private BiConsumer<? super T, ? super Throwable> traceBiConsumer(
      BiConsumer<? super T, ? super Throwable> action) {
    if (!DEEP_TRACING_ENABLED) {
      return action;
    }

    return (t, throwable) -> {
      Span span = startSpan("futureWhenComplete");
      try (Scope ignored = span.makeCurrent()) {
        action.accept(t, throwable);
      } finally {
        span.end();
      }
    };
  }

  /**
   * Syntactic sugar for {@link #thenCompose(Function, Executor)} with the function ignoring the
   * input.
   */
  public <U> InternalFuture<U> thenSupply(Supplier<InternalFuture<U>> supplier, Executor executor) {
    return thenCompose(ignored -> supplier.get(), executor);
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

  public static <U> InternalFuture<U> retriableStage(
      Supplier<InternalFuture<U>> supplier,
      Function<Throwable, Boolean> retryChecker,
      Executor executor) {
    final var promise = new CompletableFuture<U>();

    supplier
        .get()
        .whenComplete(
            (value, throwable) -> {
              boolean retryCheckerFlag = retryChecker.apply(throwable);
              if (throwable == null) {
                promise.complete(value);
              } else if (retryCheckerFlag) {
                // If we should retry, then perform the retry and map the result of the future to
                // the current promise
                // This build up a chain of promises, which can consume memory. I couldn't figure
                // out how to do better
                // with Java constraints of final vars in lambdas and not using uninitialized
                // variables.
                retriableStage(supplier, retryChecker, executor)
                    .whenComplete(
                        (v, t) -> {
                          if (t == null) {
                            promise.complete(v);
                          } else {
                            promise.completeExceptionally(t);
                          }
                        },
                        executor);
              } else {
                promise.completeExceptionally(throwable);
              }
            },
            executor);

    return from(promise);
  }

  public T get() {
    try {
      return stage.toCompletableFuture().get();
    } catch (ExecutionException ex) {
      throw new ModelDBException(ex);
    } catch (InterruptedException ex) {
      // Restore interrupted state...
      Thread.currentThread().interrupt();
      throw new ModelDBException(ex);
    }
  }

  public CompletionStage<T> toCompletionStage() {
    return stage;
  }

  public static class WithCachedExecutor<T> extends InternalFuture<T> {

    private Executor cachedExecutor = null;

    private WithCachedExecutor(CompletionStage<T> stage) {
      super(stage);
    }

    @Override
    public WithCachedExecutor<T> thenWithExecutor(Executor executor) {
      cachedExecutor = executor;
      return this;
    }

    public <U> WithCachedExecutor<U> thenCompose(Function<? super T, InternalFuture<U>> fn) {
      Objects.requireNonNull(
          cachedExecutor, "Cached executor required to use this method signature");
      return thenCompose(fn, cachedExecutor);
    }

    @Override
    @Deprecated
    public <U> WithCachedExecutor<U> thenCompose(
        Function<? super T, InternalFuture<U>> fn, Executor executor) {
      return super.thenCompose(fn, executor).thenWithExecutor(cachedExecutor);
    }

    public <U> WithCachedExecutor<U> thenApply(Function<? super T, ? extends U> fn) {
      Objects.requireNonNull(
          cachedExecutor, "Cached executor required to use this method signature");
      return thenApply(fn, cachedExecutor);
    }

    @Override
    @Deprecated
    public <U> WithCachedExecutor<U> thenApply(
        Function<? super T, ? extends U> fn, Executor executor) {
      return (WithCachedExecutor<U>) super.thenApply(fn, executor).thenWithExecutor(cachedExecutor);
    }

    public <U> WithCachedExecutor<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
      Objects.requireNonNull(
          cachedExecutor, "Cached executor required to use this method signature");
      return handle(fn, cachedExecutor);
    }

    @Override
    @Deprecated
    public <U> WithCachedExecutor<U> handle(
        BiFunction<? super T, Throwable, ? extends U> fn, Executor executor) {
      return (WithCachedExecutor<U>) super.handle(fn, executor).thenWithExecutor(cachedExecutor);
    }

    public <U, V> WithCachedExecutor<V> thenCombine(
        InternalFuture<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn) {
      Objects.requireNonNull(
          cachedExecutor, "Cached executor required to use this method signature");
      return thenCombine(other, fn, cachedExecutor);
    }

    @Override
    @Deprecated
    public <U, V> WithCachedExecutor<V> thenCombine(
        InternalFuture<? extends U> other,
        BiFunction<? super T, ? super U, ? extends V> fn,
        Executor executor) {
      return (WithCachedExecutor<V>)
          super.thenCombine(other, fn, executor).thenWithExecutor(cachedExecutor);
    }

    public WithCachedExecutor<Void> thenAccept(Consumer<? super T> action) {
      Objects.requireNonNull(
          cachedExecutor, "Cached executor required to use this method signature");
      return thenAccept(action, cachedExecutor);
    }

    @Override
    @Deprecated
    public WithCachedExecutor<Void> thenAccept(Consumer<? super T> action, Executor executor) {
      return super.thenAccept(action, executor).thenWithExecutor(executor);
    }

    public <U> WithCachedExecutor<Void> thenRun(Runnable runnable) {
      Objects.requireNonNull(
          cachedExecutor, "Cached executor required to use this method signature");
      return thenRun(runnable, cachedExecutor);
    }

    @Override
    @Deprecated
    public <U> WithCachedExecutor<Void> thenRun(Runnable runnable, Executor executor) {
      return super.thenRun(runnable, executor).thenWithExecutor(cachedExecutor);
    }

    public WithCachedExecutor<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
      Objects.requireNonNull(
          cachedExecutor, "Cached executor required to use this method signature");
      return whenComplete(action, cachedExecutor);
    }

    @Override
    @Deprecated
    public WithCachedExecutor<T> whenComplete(
        BiConsumer<? super T, ? super Throwable> action, Executor executor) {
      return super.whenComplete(action, executor).thenWithExecutor(cachedExecutor);
    }

    public <U> WithCachedExecutor<U> thenSupply(Supplier<InternalFuture<U>> supplier) {
      Objects.requireNonNull(
          cachedExecutor, "Cached executor required to use this method signature");
      return thenSupply(supplier, cachedExecutor);
    }

    @Override
    @Deprecated
    public <U> WithCachedExecutor<U> thenSupply(
        Supplier<InternalFuture<U>> supplier, Executor executor) {
      return super.thenSupply(supplier, executor).thenWithExecutor(cachedExecutor);
    }
  }

  public static class FactoryWithExecutor {

    private Executor executor;

    private FactoryWithExecutor(Executor executor) {
      this.executor = executor;
    }

    public <U> WithCachedExecutor<U> completedInternalFuture(U value) {
      return InternalFuture.completedInternalFuture(value).thenWithExecutor(executor);
    }

    public <U> WithCachedExecutor<U> from(CompletionStage<U> stage) {
      return InternalFuture.from(stage).thenWithExecutor(executor);
    }

    public <U> WithCachedExecutor<U> from(ListenableFuture<U> listenableFuture) {
      return InternalFuture.from(listenableFuture, executor).thenWithExecutor(executor);
    }

    public WithCachedExecutor<Void> runAsync(Runnable runnable) {
      return InternalFuture.runAsync(runnable, executor).thenWithExecutor(executor);
    }

    public <U> WithCachedExecutor<List<U>> sequence(final List<InternalFuture<U>> futures) {
      return InternalFuture.sequence(futures, executor).thenWithExecutor(executor);
    }

    public <U> WithCachedExecutor<U> supplyAsync(Supplier<U> supplier) {
      return InternalFuture.supplyAsync(supplier, executor).thenWithExecutor(executor);
    }

    public <U> InternalFuture<U> failedStage(Throwable ex) {
      return InternalFuture.<U>failedStage(ex).thenWithExecutor(executor);
    }

    public <U> WithCachedExecutor<U> retriableStage(
        Supplier<InternalFuture<U>> supplier, Function<Throwable, Boolean> retryChecker) {
      return InternalFuture.retriableStage(supplier, retryChecker, executor)
          .thenWithExecutor(executor);
    }
  }

  // Callback for a ListenableFuture to satisfy a promise
  private static class Callback<T> implements FutureCallback<T> {
    final CompletableFuture<T> promise;

    private Callback(CompletableFuture<T> promise) {
      this.promise = promise;
    }

    @Override
    public void onSuccess(T t) {
      promise.complete(t);
    }

    @Override
    public void onFailure(Throwable t) {
      promise.completeExceptionally(t);
    }
  }
}
