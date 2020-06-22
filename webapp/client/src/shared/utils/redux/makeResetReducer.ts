import { Reducer, Action } from 'redux';

const makeResetReducer = <ActionType extends string, State>(
  resetActionType: ActionType,
  reducer: Reducer<State>
) => (state: any, action: Action): State => {
  if (action.type === resetActionType) {
    return reducer(undefined, action);
  }
  return reducer(state, action);
};

export default makeResetReducer;
