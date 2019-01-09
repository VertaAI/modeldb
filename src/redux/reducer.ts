import { Action, combineReducers, Reducer } from 'redux';
import { IProjectsLoadAction, ProjectAction, ProjectActionName } from './actions';
import { initialState, ModelDBState } from './state';

function projectReducer(state: ModelDBState = initialState, action: ProjectAction) {
  if (action.type === ProjectActionName.LOAD_PROJECTS) {
    return {
      ...state,
      projects: (action as IProjectsLoadAction).projects,
    };
  }
  return state;
}

export default combineReducers({ projectReducer });
