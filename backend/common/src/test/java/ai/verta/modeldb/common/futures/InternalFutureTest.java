package ai.verta.modeldb.common.futures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.grpc.Context;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class InternalFutureTest {
  @Test
  void composition_failsFast() {
    FutureExecutor executor = FutureExecutor.newSingleThreadExecutor();
    InternalFuture<String> testFuture =
        InternalFuture.completedInternalFuture("cheese")
            .thenApply(
                s1 -> {
                  throw new RuntimeException("borken");
                },
                executor)
            .thenCompose(
                s -> {
                  Assertions.fail("should never execute the next stage");
                  return InternalFuture.failedStage(new RuntimeException("broken"));
                },
                executor);

    assertThatThrownBy(testFuture::get).isInstanceOf(RuntimeException.class).hasMessage("borken");
  }

  @Test
  void thenSupply() throws Exception {
    AtomicBoolean firstWasCalled = new AtomicBoolean();
    FutureExecutor executor = FutureExecutor.newSingleThreadExecutor();
    InternalFuture<Void> testFuture =
        InternalFuture.supplyAsync(
            () -> {
              firstWasCalled.set(true);
              return null;
            },
            executor);
    InternalFuture<String> result =
        testFuture.thenSupply(() -> InternalFuture.completedInternalFuture("cheese"), executor);
    assertThat(result.get()).isEqualTo("cheese");
    assertThat(firstWasCalled).isTrue();
  }

  @Test
  void thenSupply_exception() {
    AtomicBoolean secondWasCalled = new AtomicBoolean();
    FutureExecutor executor = FutureExecutor.newSingleThreadExecutor();
    InternalFuture<Void> testFuture =
        InternalFuture.supplyAsync(
            () -> {
              throw new IllegalStateException("failed");
            },
            executor);
    InternalFuture<String> result =
        testFuture.thenSupply(
            () ->
                InternalFuture.supplyAsync(
                    () -> {
                      secondWasCalled.set(true);
                      return "cheese";
                    },
                    executor),
            executor);
    assertThatThrownBy(result::get)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("failed");
    assertThat(secondWasCalled).isFalse();
  }

  @Test
  @Timeout(2)
  void retry_retryCheckerThrows() {
    FutureExecutor executor = FutureExecutor.newSingleThreadExecutor();
    assertThatThrownBy(
            () ->
                InternalFuture.retriableStage(
                        () -> InternalFuture.failedStage(new NullPointerException()),
                        throwable -> {
                          // retry checker throws an exception....
                          throw new RuntimeException("uh oh!");
                        },
                        executor)
                    .get())
        .isInstanceOf(ExecutionException.class)
        .hasRootCauseInstanceOf(RuntimeException.class)
        .hasRootCauseMessage("uh oh!");
  }

  @Test
  @Timeout(2)
  void sequence_exceptionHandling() {
    FutureExecutor executor = FutureExecutor.newSingleThreadExecutor();
    assertThatThrownBy(
            () ->
                InternalFuture.sequence(
                        List.of(InternalFuture.failedStage(new IOException("io failed"))), executor)
                    .get())
        .isInstanceOf(IOException.class)
        .hasMessage("io failed");
  }

  @Test
  void flipOptional() throws Exception {
    FutureExecutor executor = FutureExecutor.newSingleThreadExecutor();

    final var res1 =
        InternalFuture.flipOptional(
                Optional.of(InternalFuture.completedInternalFuture("123")), executor)
            .get();
    assertThat(res1).isPresent().hasValue("123");

    final var res2 = InternalFuture.flipOptional(Optional.empty(), executor).get();
    assertThat(res2).isEmpty();
  }

  @Test
  void recover() throws Exception {
    FutureExecutor executor = FutureExecutor.newSingleThreadExecutor();

    final var value =
        InternalFuture.completedInternalFuture(123)
            .thenApply(
                v -> {
                  if (true) {
                    throw new RuntimeException();
                  }
                  return 123;
                },
                executor)
            .recover(t -> 456, executor)
            .get();

    assertThat(value).isEqualTo(456);
  }

  @Test
  void fireAndForget() {
    FutureExecutor executor = FutureExecutor.newSingleThreadExecutor();
    AtomicBoolean executed = new AtomicBoolean();
    AtomicReference<String> forgottenResult = new AtomicReference<>();
    InternalFuture.runAsync(() -> forgottenResult.set("complete!"), executor)
        .whenComplete((u, throwable) -> executed.set(true), executor);

    await().until(executed::get);
    assertThat(forgottenResult).hasValue("complete!");
  }

    @Test
    public void context() throws Exception {
        final var rootContext = Context.ROOT;
        final var executor = FutureExecutor.initializeExecutor(10);
        final Context.Key<String> key = Context.key("key");
        rootContext.call(() -> {
            assertEquals(null, key.get());
            final var context1 = rootContext.withValue(key, "1");
            final var future1 = context1.call(() -> {
                assertEquals("1", key.get());
                return InternalFuture.supplyAsync(() -> {
                    assertEquals("1", key.get());
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return key.get();
                }, executor);
            });
            final var context2 = context1.withValue(key, "2");
            final var future2 = context2.call(() -> {
                assertEquals("2", key.get());
                return future1.thenApply(val -> {
                    assertEquals("2", key.get());
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return key.get();
                }, executor);
            });
            assertEquals("2", future2.get());
            return null;
        });
    }
}
