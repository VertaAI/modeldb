import { connectRouter, RouterState } from 'connected-react-router';
import { History } from 'history';
import { Action, AnyAction, combineReducers, Dispatch } from 'redux';
import { ThunkAction } from 'redux-thunk';
import { collaborationReducer, ICollaborationState } from './collaboration';
import { experimentRunsReducer, IExperimentRunsState } from './experiment-runs';
import { filtersReducer, IFilterState } from './filter';
import { IDashboardConfigState, dashboardConfigReducer } from './dashboard-config';
import { IModelRecordState, modelRecordReducer } from './model-record';
import { IProjectsState, projectsReducer } from './projects';
import { IUserState, userReducer } from './user';

export interface IApplicationState {
  collaboration: ICollaborationState;
  dashboardConfig: IDashboardConfigState;
  experimentRuns: IExperimentRunsState;
  layout: IUserState;
  modelRecord: IModelRecordState;
  projects: IProjectsState;
  router?: RouterState;
  filters: IFilterState;
}

// Additional props for connected React components. This prop is passed by default with `connect()`
export interface IConnectedReduxProps<A extends Action = any> {
  dispatch: Dispatch<A>;
}

export const createRootReducer = (history: History) =>
  combineReducers<IApplicationState>({
    collaboration: collaborationReducer,
    dashboardConfig: dashboardConfigReducer,
    experimentRuns: experimentRunsReducer,
    filters: filtersReducer,
    layout: userReducer,
    modelRecord: modelRecordReducer,
    projects: projectsReducer,
    router: connectRouter(history)
  });

export type ActionResult<R = void, A extends Action = AnyAction> = ThunkAction<R, IApplicationState, undefined, A>;
