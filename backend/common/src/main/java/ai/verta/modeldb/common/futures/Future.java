package ai.verta.modeldb.common.futures;

import ai.verta.modeldb.common.exceptions.ModelDBException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.*;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class Future<T> {
  /**
   * This instance is the global executor for all Futures. Everything will fail if it is not set
   * properly.
   */
  private static FutureExecutor futureExecutor;

  private static final boolean DEEP_TRACING_ENABLED;
  public static final AttributeKey<String> STACK_ATTRIBUTE_KEY = AttributeKey.stringKey("stack");

  static {
    String internalFutureTracingEnabled = System.getenv("IFUTURE_TRACING_ENABLED");
    DEEP_TRACING_ENABLED = Boolean.parseBoolean(internalFutureTracingEnabled);
  }

  private final Tracer futureTracer = GlobalOpenTelemetry.getTracer("futureTracer");
  private final String formattedStack;
  private final CompletionStage<T> stage;

  private Future(CompletionStage<T> stage) {
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
    this.stage = stage;
  }

  // Convert a list of futures to a future of a list
  @SuppressWarnings("unchecked")
  public static <T> Future<List<T>> sequence(final List<Future<T>> futures) {
    if (futures.isEmpty()) {
      return Future.of(List.of());
    }
    final var executor = futureExecutor.captureContext();
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

    return Future.from(promise);
  }

  static <R> Future<R> from(CompletionStage<R> other) {
    Preconditions.checkNotNull(
        futureExecutor, "A FutureExecutor is required to create a new Future.");
    return new Future<>(other);
  }

  /** @deprecated Use {@link #of(Object)} instead. */
  @Deprecated
  public static <R> Future<R> completedInternalFuture(R value) {
    Preconditions.checkNotNull(
        futureExecutor, "A FutureExecutor is required to create a new Future.");
    return of(value);
  }

  public static <R> Future<R> of(R value) {
    Preconditions.checkNotNull(
        futureExecutor, "A FutureExecutor is required to create a new Future.");
    return new Future<>(CompletableFuture.completedFuture(value));
  }

  public <U> Future<U> thenCompose(Function<? super T, Future<U>> fn) {
    Preconditions.checkNotNull(
        futureExecutor, "A FutureExecutor is required to be present for this method.");
    final var executor = futureExecutor.captureContext();
    return from(
        stage.thenComposeAsync(
            traceFunction(
                traceFunction(
                    fn.andThen(internalFuture -> internalFuture.stage), "futureThenCompose"),
                "futureThenApply"),
            executor));
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

  public <U> Future<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
    Preconditions.checkNotNull(
        futureExecutor,
        "A FutureExecutor is required to be present for this method. Please call withExecutor before calling this.");
    final var executor = futureExecutor.captureContext();
    return from(stage.handleAsync(traceBiFunctionThrowable(fn), executor));
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

  public <U, V> Future<V> thenCombine(
      Future<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn) {
    Preconditions.checkNotNull(
        futureExecutor,
        "A FutureExecutor is required to be present for this method. Please call withExecutor before calling this.");
    final var executor = futureExecutor.captureContext();
    BiFunction<? super T, ? super U, ? extends V> tracedBiFunction = traceBiFunction(fn);
    return from(stage.thenCombineAsync(other.stage, tracedBiFunction, executor));
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

  public Future<Void> thenAccept(Consumer<? super T> action) {
    Preconditions.checkNotNull(
        futureExecutor,
        "A FutureExecutor is required to be present for this method. Please call withExecutor before calling this.");
    final var executor = futureExecutor.captureContext();
    return from(stage.thenAcceptAsync(traceConsumer(action), executor));
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

  public Future<Void> thenRun(Runnable runnable) {
    Preconditions.checkNotNull(
        futureExecutor,
        "A FutureExecutor is required to be present for this method. Please call withExecutor before calling this.");
    final var executor = futureExecutor.captureContext();
    return from(stage.thenRunAsync(traceRunnable(runnable), executor));
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

  public Future<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
    final var executor = futureExecutor.captureContext();
    return from(stage.whenCompleteAsync(traceBiConsumer(action), executor));
  }

  public Future<T> recover(Function<Throwable, ? extends T> fn) {
    return handle(
        (v, t) -> {
          if (t != null) {
            return fn.apply(t);
          }
          return v;
        });
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

  /** Syntactic sugar for {@link #thenCompose(Function)} with the function ignoring the input. */
  public <U> Future<U> thenSupply(Supplier<Future<U>> supplier) {
    Preconditions.checkNotNull(
        futureExecutor,
        "A FutureExecutor is required to be present for this method. Please call withExecutor before calling this.");
    return thenCompose(ignored -> supplier.get());
  }

  public static Future<Void> runAsync(Runnable runnable) {
    final var executor = futureExecutor.captureContext();
    return from(CompletableFuture.runAsync(runnable, executor));
  }

  public static <U> Future<U> supplyAsync(Supplier<U> supplier) {
    final var executor = futureExecutor.captureContext();
    return from(CompletableFuture.supplyAsync(supplier, executor));
  }

  public static <U> Future<U> failedStage(Throwable ex) {
    return from(CompletableFuture.failedFuture(ex));
  }

  public static <U> Future<U> retriableStage(
      Supplier<Future<U>> supplier, Function<Throwable, Boolean> retryChecker) {
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
                retriableStage(supplier, retryChecker)
                    .whenComplete(
                        (v, t) -> {
                          if (t == null) {
                            promise.complete(v);
                          } else {
                            promise.completeExceptionally(t);
                          }
                        });
              } else {
                promise.completeExceptionally(throwable);
              }
            });

    return Future.from(promise);
  }

  public static <U> Future<Optional<U>> flipOptional(Optional<Future<U>> val) {
    return val.map(future -> future.thenCompose(u -> Future.of(Optional.ofNullable(u))))
        .orElse(Future.of(Optional.empty()));
  }

  public T get() throws Exception {
    try {
      return stage.toCompletableFuture().get();
    } catch (ExecutionException ex) {
      Throwable cause = ex.getCause();
      if (cause instanceof Exception) {
        throw (Exception) cause;
      }
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

  public static void setFutureExecutor(FutureExecutor executor) {
    if (futureExecutor == null) {
      futureExecutor = executor;
    } else {
      log.warn(
          "A FutureExecutor has already been set. Ignoring this call.",
          new RuntimeException("call-site capturer"));
    }
  }

  /** @deprecated Only use this as a part of the conversion process between versions of Futures. */
  @Deprecated
  public InternalFuture<T> toInternalFuture() {
    return InternalFuture.from(stage);
  }

  @VisibleForTesting
  static void resetExecutorForTest() {
    futureExecutor = null;
  }
}
