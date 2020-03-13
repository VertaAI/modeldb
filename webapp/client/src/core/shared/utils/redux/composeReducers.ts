import { Action, Reducer } from 'redux';

export default function composeReducers<S>(reducers: Array<Reducer<S>>) {
  return <A extends Action>(state: S | undefined, action: A) =>
    reducers.reduce(
      (_state: S | undefined, reducer: Reducer<S>) => reducer(_state, action),
      state
    ) as S;
}
