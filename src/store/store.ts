import { connectRouter, RouterState } from 'connected-react-router';
import { History } from 'history';
import { Action, AnyAction, combineReducers, Dispatch } from 'redux';
import { ThunkAction } from 'redux-thunk';
import { IExperimentRunsState, experimentRunsReducer } from './experiment-runs';
import { IProjectsState, projectsReducer } from './project';
import { IUserState, userReducer } from './user';

export interface IApplicationState {
  layout: IUserState;
  router?: RouterState;
  projects: IProjectsState;
  experimentRuns: IExperimentRunsState;
}

// Additional props for connected React components. This prop is passed by default with `connect()`
export interface IConnectedReduxProps<A extends Action = any> {
  dispatch: Dispatch<A>;
}

export const createRootReducer = (history: History) =>
  combineReducers<IApplicationState>({
    layout: userReducer,
    router: connectRouter(history),
    projects: projectsReducer,
    experimentRuns: experimentRunsReducer
  });

export type ActionResult<R = void, A extends Action = AnyAction> = ThunkAction<R, IApplicationState, undefined, A>;
