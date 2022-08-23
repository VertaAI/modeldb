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
        InternalFuture.withExecutor(executor)
            .completedInternalFuture("cheese")
            .thenApply(
                s1 -> {
                  throw new RuntimeException("borken");
                })
            .thenCompose(
                s -> {
                  Assertions.fail("should never execute the next stage");
                  return InternalFuture.failedStage(new RuntimeException("broken"));
                });

    assertThatThrownBy(testFuture::get).getRootCause().hasMessage("borken");
  }

  @Test
  void thenSupply() {
    AtomicBoolean firstWasCalled = new AtomicBoolean();
    Executor executor = MoreExecutors.directExecutor();
    var factory = InternalFuture.withExecutor(executor);
    var testFuture =
        factory.supplyAsync(
            () -> {
              firstWasCalled.set(true);
              return null;
            });
    InternalFuture<String> result =
        testFuture.thenSupply(() -> factory.completedInternalFuture("cheese"));
    assertThat(result.get()).isEqualTo("cheese");
    assertThat(firstWasCalled).isTrue();
  }

  @Test
  void thenSupply_exception() {
    AtomicBoolean secondWasCalled = new AtomicBoolean();
    Executor executor = MoreExecutors.directExecutor();
    var factory = InternalFuture.withExecutor(executor);
    var testFuture =
        factory.supplyAsync(
            () -> {
              throw new IllegalStateException("failed");
            });
    InternalFuture<String> result =
        testFuture.thenSupply(
            () ->
                factory.supplyAsync(
                    () -> {
                      secondWasCalled.set(true);
                      return "cheese";
                    }));
    assertThatThrownBy(result::get).hasMessageContaining("failed");
    assertThat(secondWasCalled).isFalse();
  }
}
