package ai.verta.modeldb.common.futures;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.Executor;
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
  void composition_syncFailsFast() {
    InternalFuture<String> testFuture =
        InternalFuture.completedInternalFuture("cheese")
            .thenApplySync(
                s1 -> {
                  throw new RuntimeException("borken");
                })
            .thenComposeSync(
                s -> {
                  Assertions.fail("should never execute the next stage");
                  return InternalFuture.failedStage(new RuntimeException("broken"));
                });

    assertThatThrownBy(testFuture::get).getRootCause().hasMessage("borken");
  }
}
