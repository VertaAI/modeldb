import { connectRouter, RouterState } from 'connected-react-router';
import { History } from 'history';
import { Action, AnyAction, combineReducers, Dispatch } from 'redux';
import { ThunkAction } from 'redux-thunk';
import { IExperimentRunsState, experimentRunsReducer } from './experiment-runs';
import { IProjectsState, projectsReducer } from './projects';
import { IModelRecordState, modelRecordReducer } from './model-record';
modelRecordReducer;
import { IUserState, userReducer } from './user';

export interface IApplicationState {
  layout: IUserState;
  router?: RouterState;
  projects: IProjectsState;
  experiment_runs: IExperimentRunsState;
  model_record: IModelRecordState;
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
    experiment_runs: experimentRunsReducer,
    model_record: modelRecordReducer
  });

export type ActionResult<R = void, A extends Action = AnyAction> = ThunkAction<R, IApplicationState, undefined, A>;
