package ai.verta.modeldb.common.futures;

import ai.verta.modeldb.common.exceptions.ModelDBException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class Future<T> {
  /**
   * Set this system property to "true" to enable capturing call-site stacks for debugging purposes.
   */
  public static final String FUTURE_TESTING_STACKS_ENABLED = "FUTURE_TESTING_STACKS_ENABLED";
  /**
   * This instance is the global executor for all Futures. Everything will fail if it is not set
   * properly.
   */
  private static FutureExecutor futureExecutor;

  private static Tracer futureTracer;

  private static final boolean DEEP_TRACING_ENABLED;
  private static boolean captureStacksAtCreation;

  public static final AttributeKey<String> STACK_ATTRIBUTE_KEY = AttributeKey.stringKey("stack");
  public static final String FUTURE_EXECUTOR_REQUIRED_ERROR =
      "A FutureExecutor is required to create a new Future.";

  static {
    String internalFutureTracingEnabled = System.getenv("IFUTURE_TRACING_ENABLED");
    DEEP_TRACING_ENABLED = Boolean.parseBoolean(internalFutureTracingEnabled);
    captureStacksAtCreation =
        Boolean.parseBoolean(System.getProperty(FUTURE_TESTING_STACKS_ENABLED));
  }

  private final String formattedStack;
  private final CompletionStage<T> stage;
  // this special Executor can be assigned for a single future, for special cases like retries.
  private FutureExecutor specialExecutor = null;

  private Future(CompletionStage<T> stage) {
    if (DEEP_TRACING_ENABLED || captureStacksAtCreation) {
      StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
      // get rid of the top of the stack, which is not useful
      stackTrace = Arrays.copyOfRange(stackTrace, 1, stackTrace.length);
      if (stackTrace.length > 10) {
        stackTrace = Arrays.copyOf(stackTrace, 10);
      }
      this.formattedStack =
          Arrays.stream(stackTrace)
              .map(StackTraceElement::toString)
              .collect(Collectors.joining("\n\tat "));
    } else {
      this.formattedStack = null;
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

  /**
   * Create a {@link Future} from an existing {@link CompletionStage}. Useful for interop with other
   * libraries.
   */
  public static <R> Future<R> from(CompletionStage<R> other) {
    Preconditions.checkNotNull(futureExecutor, FUTURE_EXECUTOR_REQUIRED_ERROR);
    return new Future<>(other);
  }

  /** @deprecated Use {@link #of(Object)} instead. */
  @Deprecated
  public static <R> Future<R> completedInternalFuture(R value) {
    Preconditions.checkNotNull(futureExecutor, FUTURE_EXECUTOR_REQUIRED_ERROR);
    return of(value);
  }

  public static <R> Future<R> of(R value) {
    Preconditions.checkNotNull(futureExecutor, FUTURE_EXECUTOR_REQUIRED_ERROR);
    return new Future<>(CompletableFuture.completedFuture(value));
  }

  /**
   * Converts a {@link ListenableFuture}, returned by a non-blocking call via grpc, to a {@link
   * Future}.
   */
  public static <T> Future<T> fromListenableFuture(ListenableFuture<T> listenableFuture) {
    Preconditions.checkNotNull(futureExecutor, FUTURE_EXECUTOR_REQUIRED_ERROR);
    CompletableFuture<T> promise = new CompletableFuture<>();
    Futures.addCallback(listenableFuture, new FutureUtil.Callback<>(promise), futureExecutor);
    return from(promise);
  }

  public Future<T> onSuccess(Consumer<T> fn) {
    final var executor = getExecutor();
    return from(
        stage.whenCompleteAsync(
            (t, throwable) -> {
              if (throwable == null) {
                fn.accept(t);
              }
            },
            executor));
  }

  public Future<T> onFailure(Consumer<Throwable> fn) {
    final var executor = getExecutor();
    return from(
        stage.whenCompleteAsync(
            (t, throwable) -> {
              if (throwable != null) {
                fn.accept(throwable);
              }
            },
            executor));
  }

  public <U> Future<U> thenCompose(Function<? super T, Future<U>> fn) {
    Preconditions.checkNotNull(
        futureExecutor, "A FutureExecutor is required to be present for this method.");
    final var executor = getExecutor();
    return from(
        stage.thenComposeAsync(
            traceFunction(
                traceFunction(
                    fn.andThen(
                        internalFuture -> {
                          if (internalFuture == null) {
                            if (formattedStack != null) {
                              log.warn(
                                  "Null thenCompose internalFuture found. Call site stack:\n "
                                      + formattedStack);
                            }
                            throw new NullPointerException(
                                "Null internalFuture found during thenCompose call");
                          }
                          return internalFuture.stage;
                        }),
                    "futureThenCompose"),
                "futureThenApply"),
            executor));
  }

  private FutureExecutor getExecutor() {
    if (specialExecutor != null) {
      return specialExecutor.captureContext();
    }
    return futureExecutor.captureContext();
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
    if (futureTracer == null) {
      return Span.getInvalid();
    }
    return futureTracer
        .spanBuilder(futureThenApply)
        .setAttribute(STACK_ATTRIBUTE_KEY, formattedStack)
        .startSpan();
  }

  public <U> Future<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
    Preconditions.checkNotNull(futureExecutor, FUTURE_EXECUTOR_REQUIRED_ERROR);
    final var executor = getExecutor();
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
    Preconditions.checkNotNull(futureExecutor, FUTURE_EXECUTOR_REQUIRED_ERROR);
    final var executor = getExecutor();
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
    Preconditions.checkNotNull(futureExecutor, FUTURE_EXECUTOR_REQUIRED_ERROR);
    final var executor = getExecutor();
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
    Preconditions.checkNotNull(futureExecutor, FUTURE_EXECUTOR_REQUIRED_ERROR);
    final var executor = getExecutor();
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
    final var executor = getExecutor();
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
    Preconditions.checkNotNull(futureExecutor, FUTURE_EXECUTOR_REQUIRED_ERROR);
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

  public static <U> Future<U> retrying(
      Supplier<Future<U>> supplier, RetryStrategy<U> retryStrategy) {
    final var promise = new CompletableFuture<U>();

    supplier
        .get()
        .whenComplete(
            (value, throwable) -> {
              RetryStrategy.Retry retry;
              try {
                retry = retryStrategy.shouldRetry(value, throwable);
              } catch (Throwable e) {
                promise.completeExceptionally(
                    new ExecutionException("retryStrategy threw an exception. Not retrying.", e));
                return;
              }
              if (!retry.shouldRetry()) {
                if (throwable == null) {
                  promise.complete(value);
                } else {
                  promise.completeExceptionally(throwable);
                }
                return;
              }
              FutureExecutor delayedExecutor;
              if (retry.getAmountToDelay() == 0) {
                delayedExecutor = futureExecutor;
              } else {
                delayedExecutor =
                    FutureExecutor.makeCompatibleExecutor(
                        CompletableFuture.delayedExecutor(
                            retry.getAmountToDelay(), retry.getTimeUnit(), futureExecutor),
                        futureExecutor.getName());
              }

              retrying(
                      () -> {
                        Future<U> newFuture = supplier.get();
                        newFuture.setSpecialExecutor(delayedExecutor);
                        return newFuture;
                      },
                      retryStrategy)
                  .whenComplete(
                      (v, t) -> {
                        if (t == null) {
                          promise.complete(v);
                        } else {
                          promise.completeExceptionally(t);
                        }
                      });
            });

    return Future.from(promise);
  }

  private void setSpecialExecutor(FutureExecutor delayedExecutor) {
    this.specialExecutor = delayedExecutor;
  }

  public static <U> Future<U> retriableStage(
      Supplier<Future<U>> supplier, Function<Throwable, Boolean> retryChecker) {
    return retrying(
        supplier,
        (x, throwable) -> {
          boolean result = throwable != null && retryChecker.apply(throwable);
          return new RetryStrategy.Retry(result, 0, TimeUnit.SECONDS);
        });
  }

  public static <U> Future<Optional<U>> flipOptional(Optional<Future<U>> val) {
    return val.map(future -> future.thenCompose(u -> Future.of(Optional.ofNullable(u))))
        .orElse(Future.of(Optional.empty()));
  }

  @Deprecated(forRemoval = true)
  public T get() throws Exception {
    return blockAndGet();
  }

  public T blockAndGet() throws Exception {
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

  public static void setOpenTelemetry(OpenTelemetry openTelemetry) {
    if (futureTracer != null) {
      log.warn(
          "OpenTelemetry has already been set. Ignoring this call.",
          new RuntimeException("call-site capturer"));
    }
    futureTracer = openTelemetry.getTracer("futureTracer");
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

  public static void enableCallSiteStackCapture() {
    captureStacksAtCreation = true;
  }

  public static void disableCallSiteStackCapture() {
    captureStacksAtCreation = false;
  }
}
