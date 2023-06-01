package ai.verta.modeldb.common.futures;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * Generic linear state machine that operates on a sequence of Future functions.
 *
 * <p>To use this, you will need to define the State for your process, which will encapsulate the
 * state that is needed for your process to operate.
 *
 * @param <T> The type of final result output by the state machine.
 * @param <S> The type of the machine state.
 */
public class FutureMachine<T, S extends FutureMachine.State<T>> {
  private final List<Step<T, S>> steps;

  public FutureMachine(List<Step<T, S>> steps) {
    this.steps = new ArrayList<>(steps);
  }

  public Future<T> process(S initialState) {
    Iterator<Step<T, S>> initialIterator = steps.iterator();
    return process(initialState, initialIterator.next(), initialIterator);
  }

  private Future<T> process(S currentState, Step<T, S> nextStep, Iterator<Step<T, S>> steps) {
    Future<S> stepResult = nextStep.apply(currentState);
    return stepResult.thenCompose(
        newState -> {
          if (newState.isComplete()) {
            return newState.getResult();
          }
          if (!steps.hasNext()) {
            // hopefully this isn't null and the state machine terminates properly!
            return currentState.getResult();
          }
          return process(newState, steps.next(), steps);
        });
  }

  public interface State<T> {
    Future<T> getResult();

    default boolean isComplete() {
      return getResult() != null;
    }
  }

  public interface Step<T, S> extends Function<S, Future<S>> {}
}
