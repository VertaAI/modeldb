package ai.verta.modeldb.common.futures;

import ai.verta.modeldb.common.exceptions.ModelDBException;
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
  private CompletionStage<T> stage;

  // keep the calling OpenTelemetry context, so we can use it to wrap the actual invocation of the
  // future's implementation
  private final Context callingContext = Context.current();

  private InternalFuture() {
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
              } catch (Throwable t) {
                if (t instanceof InterruptedException) {
                  // Restore interrupted state...
                  Thread.currentThread().interrupt();
                }
                promise.completeExceptionally(t);
              }
            },
            executor);

    return InternalFuture.from(promise);
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
        stage.thenComposeAsync(
            traceFunction(
                callingContext.wrapFunction(
                    traceFunction(
                        fn.andThen(internalFuture -> internalFuture.stage), "futureThenCompose")),
                "futureThenApply"),
            executor));
  }

  public <U> InternalFuture<U> thenApply(Function<? super T, ? extends U> fn, Executor executor) {
    return from(
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
    return from(
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
    return from(
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
              boolean retryCheckerFlag;
              try {
                retryCheckerFlag = retryChecker.apply(throwable);
              } catch (Throwable e) {
                promise.completeExceptionally(
                    new ExecutionException("retryChecker threw an exception. Not retrying.", e));
                return;
              }
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

    return InternalFuture.from(promise);
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
}
