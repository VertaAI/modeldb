package ai.verta.modeldb.common.futures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import lombok.Value;
import lombok.With;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FutureMachineTest {

  @BeforeAll
  static void beforeAll() {
    FutureExecutor executor = FutureExecutor.newSingleThreadExecutor();
    Future.setFutureExecutor(executor);
  }

  @Test
  void process() throws Exception {
    FutureMachine<String, TestState> machine =
        new FutureMachine<>(
            List.of(
                state -> Future.of(state.withSomeNonIdempotentState("intermediate value")),
                state -> Future.of(state.withSomeOtherState(5)),
                state -> Future.of(state.withBooleanState(true)),
                state ->
                    Future.of(
                        state.withFinalResult(
                            state.someState
                                + " "
                                + state.someOtherState
                                + " "
                                + state.booleanState))));
    Future<String> result = machine.process(TestState.empty);
    assertThat(result.blockAndGet()).isEqualTo("intermediate value 5 true");
  }

  @Test
  void process_failedFuture() {
    FutureMachine<String, TestState> machine =
        new FutureMachine<>(
            List.of(
                state -> Future.of(state.withSomeState("intermediate value")),
                state -> Future.failedStage(new RuntimeException("failed")),
                state -> Future.of(state.withBooleanState(true)),
                state ->
                    Future.of(
                        state.withFinalResult(
                            state.someState
                                + " "
                                + state.someOtherState
                                + " "
                                + state.booleanState))));
    Future<String> result = machine.process(TestState.empty);
    assertThatThrownBy(result::blockAndGet)
        .isInstanceOf(RuntimeException.class)
        .hasMessage("failed");
  }

  @Test
  void process_thrownException() {
    FutureMachine<String, TestState> machine =
        new FutureMachine<>(
            List.of(
                s -> Future.of(s.withSomeState("intermediate value")),
                s -> {
                  throw new RuntimeException("failed");
                },
                s -> Future.of(s.withBooleanState(true)),
                s ->
                    Future.of(
                        s.withFinalResult(
                            s.someState + " " + s.someOtherState + " " + s.booleanState))));
    Future<String> result = machine.process(TestState.empty);
    assertThatThrownBy(result::blockAndGet)
        .isInstanceOf(RuntimeException.class)
        .hasMessage("failed");
  }

  @Value
  @With
  private static class TestState implements FutureMachine.State<String> {
    static TestState empty = new TestState(null, null, null, false);

    String finalResult;
    String someState;
    Integer someOtherState;
    boolean booleanState;

    @Override
    public Future<String> getResult() {
      return finalResult == null ? null : Future.of(finalResult);
    }

    public TestState withSomeNonIdempotentState(String state) {
      // make this non-idempotent to test a bugfix
      if (someState != null) {
        throw new IllegalStateException("some state has already been set!");
      }
      return withSomeState(state);
    }
  }
}
