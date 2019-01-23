import { connectRouter, RouterState } from 'connected-react-router';
import { History } from 'history';
import { Action, AnyAction, combineReducers, Dispatch } from 'redux';
import { ThunkAction } from 'redux-thunk';
import { IModelState, modelReducer } from './model';
import { IProjectsState, IProjectState, projectReducer, projectsReducer } from './project';
import { IUserState, userReducer } from './user';

export interface IApplicationState {
  layout: IUserState;
  projects: IProjectsState;
  router?: RouterState;
  project: IProjectState;
  model: IModelState;
}

// Additional props for connected React components. This prop is passed by default with `connect()`
export interface IConnectedReduxProps<A extends Action = any> {
  dispatch: Dispatch<A>;
}

export const createRootReducer = (history: History) =>
  combineReducers<IApplicationState>({
    layout: userReducer,
    model: modelReducer,
    project: projectReducer,
    projects: projectsReducer,
    router: connectRouter(history)
  });

export type ActionResult<R = void, A extends Action = AnyAction> = ThunkAction<R, IApplicationState, undefined, A>;
