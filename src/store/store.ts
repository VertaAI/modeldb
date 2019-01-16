import { connectRouter, RouterState } from 'connected-react-router';
import { History } from 'history';
import { Action, AnyAction, combineReducers, Dispatch } from 'redux';
import { ThunkAction } from 'redux-thunk';
import { ILayoutState, layoutReducer } from './layout';
import { IProjectsState, IProjectState, projectReducer, projectsReducer } from './project';

export interface IApplicationState {
  layout: ILayoutState;
  projects: IProjectsState;
  router?: RouterState;
  project: IProjectState;
}

// Additional props for connected React components. This prop is passed by default with `connect()`
export interface IConnectedReduxProps<A extends Action = any> {
  dispatch: Dispatch<A>;
}

export const createRootReducer = (history: History) =>
  combineReducers<IApplicationState>({
    layout: layoutReducer,
    project: projectReducer,
    projects: projectsReducer,
    router: connectRouter(history)
  });

export type ActionResult<R = void, A extends Action = AnyAction> = ThunkAction<R, IApplicationState, undefined, A>;

// export const someThunkAction = (): ActionResult<Promise<boolean>> => async (dispatch, getState) => {
//   const state = getState();
//   await Promise.resolve('some result');
//   dispatch(fetchProjects());
//   return Promise.resolve(true);
// };
