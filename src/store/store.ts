import { connectRouter, RouterState } from 'connected-react-router';
import { History } from 'history';
import { Action, AnyAction, combineReducers, Dispatch } from 'redux';
import { ILayoutState, layoutReducer } from './layout';
import { IProjectsState, projectReducer } from './project';

export interface IApplicationState {
  layout: ILayoutState;
  projects: IProjectsState;
  router?: RouterState;
}

// Additional props for connected React components. This prop is passed by default with `connect()`
export interface IConnectedReduxProps<A extends Action = AnyAction> {
  dispatch: Dispatch<A>;
}

export const createRootReducer = (history: History) =>
  combineReducers<IApplicationState>({
    layout: layoutReducer,
    projects: projectReducer,
    router: connectRouter(history)
  });
