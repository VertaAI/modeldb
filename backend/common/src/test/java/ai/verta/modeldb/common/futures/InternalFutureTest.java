package ai.verta.modeldb.common.futures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.verta.modeldb.common.exceptions.ModelDBException;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class InternalFutureTest {
  @Test
  void composition_failsFast() {
    Executor executor = MoreExecutors.directExecutor();
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
    Executor executor = MoreExecutors.directExecutor();
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
    Executor executor = MoreExecutors.directExecutor();
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
    ExecutorService executor = Executors.newSingleThreadExecutor();
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
    ExecutorService executor = Executors.newSingleThreadExecutor();
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
    ExecutorService executor = Executors.newSingleThreadExecutor();

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
    ExecutorService executor = Executors.newSingleThreadExecutor();

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
}
