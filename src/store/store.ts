import { connectRouter, RouterState } from 'connected-react-router';
import { History } from 'history';
import { Action, AnyAction, combineReducers, Dispatch } from 'redux';
import { ThunkAction } from 'redux-thunk';
import { ILayoutState, layoutReducer } from './layout';

export interface IApplicationState {
  layout: ILayoutState;
  router?: RouterState;
}

// Additional props for connected React components. This prop is passed by default with `connect()`
export interface IConnectedReduxProps<A extends Action = any> {
  dispatch: Dispatch<A>;
}

export const createRootReducer = (history: History) =>
  combineReducers<IApplicationState>({
    layout: layoutReducer,
    router: connectRouter(history)
  });

export type ActionResult<R = void> = ThunkAction<R, IApplicationState, undefined, AnyAction>;

// export const someThunkAction = (): ActionResult<Promise<boolean>> => async (dispatch, getState) => {
//   const state = getState();
//   await Promise.resolve('some result');
//   dispatch(fetchProjects());
//   return Promise.resolve(true);
// };
