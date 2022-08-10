package ai.verta.modeldb.common.futures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

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

    assertThatThrownBy(testFuture::get).getRootCause().hasMessage("borken");
  }

  @Test
  void thenSupply() {
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
    assertThatThrownBy(result::get).hasMessageContaining("failed");
    assertThat(secondWasCalled).isFalse();
  }
}
