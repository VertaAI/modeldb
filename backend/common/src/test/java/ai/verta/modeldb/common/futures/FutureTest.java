package ai.verta.modeldb.common.futures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.Context;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

class FutureTest {
  @RegisterExtension
  static final OpenTelemetryExtension otelTesting = OpenTelemetryExtension.create();

  @AfterEach
  void tearDown() {
    Future.resetExecutorForTest();
  }

  @BeforeEach
  void setUp() {
    FutureExecutor executor = FutureExecutor.newSingleThreadExecutor();
    Future.setFutureExecutor(executor);
  }

  @Test
  void executorRequired() {
    Future.resetExecutorForTest();
    assertThatThrownBy(() -> Future.of("foo"));
    assertThatThrownBy(() -> Future.failedStage(new RuntimeException()));
    assertThatThrownBy(() -> Future.supplyAsync(() -> "foo"));
    assertThatThrownBy(() -> Future.runAsync(() -> {}));
    assertThatThrownBy(() -> Future.sequence(List.of()));

    Future.setFutureExecutor(FutureExecutor.newSingleThreadExecutor());
    Future<String> baseFuture = Future.of("foo");
    Future.resetExecutorForTest();
    assertThatThrownBy(() -> Future.retriableStage(() -> baseFuture, throwable -> true));
    assertThatThrownBy(() -> Future.flipOptional(Optional.of(baseFuture)));
  }

  @Test
  void composition_failsFast() {
    Future<String> testFuture =
        Future.of("cheese")
            .thenCompose(
                s1 -> {
                  throw new RuntimeException("borken");
                })
            .thenCompose(
                s -> {
                  Assertions.fail("should never execute the next stage");
                  return Future.failedStage(new RuntimeException("broken"));
                });

    assertThatThrownBy(testFuture::get).isInstanceOf(RuntimeException.class).hasMessage("borken");
  }

  @Test
  void thenSupply() throws Exception {
    AtomicBoolean firstWasCalled = new AtomicBoolean();
    Future<Void> testFuture =
        Future.supplyAsync(
            () -> {
              firstWasCalled.set(true);
              return null;
            });
    Future<String> result = testFuture.thenSupply(() -> Future.of("cheese"));
    assertThat(result.get()).isEqualTo("cheese");
    assertThat(firstWasCalled).isTrue();
  }

  @Test
  void thenSupply_exception() {
    AtomicBoolean secondWasCalled = new AtomicBoolean();
    Future<Void> testFuture =
        Future.supplyAsync(
            () -> {
              throw new IllegalStateException("failed");
            });
    Future<String> result =
        testFuture.thenSupply(
            () ->
                Future.supplyAsync(
                    () -> {
                      secondWasCalled.set(true);
                      return "cheese";
                    }));
    assertThatThrownBy(result::get)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("failed");
    assertThat(secondWasCalled).isFalse();
  }

  @Test
  @Timeout(2)
  void retry_retryCheckerThrows() {
    assertThatThrownBy(
            () ->
                Future.retriableStage(
                        () -> Future.failedStage(new NullPointerException()),
                        throwable -> {
                          // retry checker throws an exception....
                          throw new RuntimeException("uh oh!");
                        })
                    .get())
        .isInstanceOf(ExecutionException.class)
        .hasRootCauseInstanceOf(RuntimeException.class)
        .hasRootCauseMessage("uh oh!");
  }

  @Test
  void retry() throws Exception {
    AtomicInteger sequencer = new AtomicInteger();

    assertThat(
            Future.retriableStage(
                    () -> {
                      if (sequencer.getAndIncrement() < 2) {
                        return Future.failedStage(new NullPointerException());
                      }
                      return Future.of("success!");
                    },
                    throwable -> true)
                .get())
        .isEqualTo("success!");
  }

  @Test
  @Timeout(2)
  void sequence_exceptionHandling() {
    assertThatThrownBy(
            () ->
                Future.sequence(
                        List.of(Future.of("one"), Future.failedStage(new IOException("io failed"))))
                    .get())
        .isInstanceOf(IOException.class)
        .hasMessage("io failed");
  }

  @Test
  void sequence() throws Exception {
    assertThat(Future.sequence(List.of(Future.of("one"), Future.of("two"))).get())
        .containsExactly("one", "two");
  }

  @Test
  void thenCombine() throws Exception {
    assertThat(Future.of("one").thenCombine(Future.of("two"), (s, s2) -> s + s2).get())
        .isEqualTo("onetwo");
  }

  @Test
  void thenAccept() throws Exception {
    AtomicReference<String> seenValue = new AtomicReference<>();
    Future.of("one").thenAccept(seenValue::set).get();
    assertThat(seenValue).hasValue("one");
  }

  @Test
  void thenRun() throws Exception {
    AtomicBoolean runnableCalled = new AtomicBoolean();
    Future.of("one").thenRun(() -> runnableCalled.set(true)).get();
    assertThat(runnableCalled).isTrue();
  }

  @Test
  void flipOptional() throws Exception {
    final var res1 = Future.flipOptional(Optional.of(Future.of("123"))).get();
    assertThat(res1).isPresent().hasValue("123");

    final var res2 = Future.flipOptional(Optional.empty()).get();
    assertThat(res2).isEmpty();
  }

  @Test
  void recover() throws Exception {
    final var value =
        Future.of(123)
            .thenCompose(
                v -> {
                  if (true) {
                    throw new RuntimeException();
                  }
                  return Future.of(123);
                })
            .recover(t -> 456)
            .get();

    assertThat(value).isEqualTo(456);
  }

  @Test
  void fireAndForget() {
    AtomicBoolean executed = new AtomicBoolean();
    AtomicReference<String> forgottenResult = new AtomicReference<>();
    Future.runAsync(() -> forgottenResult.set("complete!"))
        .whenComplete((u, throwable) -> executed.set(true));

    await().until(executed::get);
    assertThat(forgottenResult).hasValue("complete!");
  }

  @Test
  void trace() throws Exception {
    Tracer tracer = otelTesting.getOpenTelemetry().getTracer("testTracer");
    Span outside = tracer.spanBuilder("outer").startSpan();
    try (Scope ignored = outside.makeCurrent()) {
      Future.supplyAsync(
              () -> {
                tracer.spanBuilder("one").startSpan().end();
                return "one";
              })
          .thenCompose(
              a ->
                  Future.supplyAsync(
                      () -> {
                        tracer.spanBuilder("two").startSpan().end();
                        return "two";
                      }))
          .thenRun(() -> tracer.spanBuilder("three").startSpan().end())
          .get();
    } finally {
      outside.end();
    }

    otelTesting
        .assertTraces()
        .hasSize(1)
        .hasTracesSatisfyingExactly(
            trace ->
                trace.hasSpansSatisfyingExactlyInAnyOrder(
                    s -> s.hasName("outer").hasEnded(),
                    s -> s.hasName("one").hasEnded(),
                    s -> s.hasName("two").hasEnded(),
                    s -> s.hasName("three").hasEnded()));
  }

  @Test
  public void context() throws Exception {
    final var rootContext = Context.ROOT;
    final Context.Key<String> key = Context.key("key");
    rootContext.call(
        () -> {
          assertThat(key.get()).isNull();
          final var context1 = rootContext.withValue(key, "1");
          final var future1 =
              context1.call(
                  () -> {
                    assertThat(key.get()).isEqualTo("1");
                    return Future.supplyAsync(
                        () -> {
                          assertThat(key.get()).isEqualTo("1");
                          try {
                            Thread.sleep(10);
                          } catch (InterruptedException e) {
                            e.printStackTrace();
                          }
                          return key.get();
                        });
                  });
          final var context2 = context1.withValue(key, "2");
          final var future2 =
              context2.call(
                  () -> {
                    assertThat(key.get()).isEqualTo("2");
                    return future1.thenCompose(
                        val -> {
                          assertThat(key.get()).isEqualTo("2");
                          try {
                            Thread.sleep(10);
                          } catch (InterruptedException e) {
                            e.printStackTrace();
                          }
                          return Future.of(key.get());
                        });
                  });
          assertThat(future2.get()).isEqualTo("2");
          return null;
        });
  }

  @Test
  void fromListenableFuture() throws Exception {
    ListenableFuture<String> a = Futures.immediateFuture("cheese");
    assertThat(Future.fromListenableFuture(a).get()).isEqualTo("cheese");

    ListenableFuture<String> b =
        Futures.submit(() -> "lemonade", Executors.newSingleThreadExecutor());
    assertThat(Future.fromListenableFuture(b).get()).isEqualTo("lemonade");

    ListenableFuture<String> c = Futures.immediateFailedFuture(new RuntimeException("oh no"));
    assertThatThrownBy(() -> Future.fromListenableFuture(c).get())
        .isInstanceOf(RuntimeException.class)
        .hasMessage("oh no");
  }
}
