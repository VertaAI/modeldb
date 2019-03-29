import { Reducer } from 'redux';

import {
  fetchProjectsAction,
  fetchProjectsActionTypes,
  IProjectsState,
  IUpdateProjectAction,
  updateProjectActionTypes,
} from './types';

const projectsInitialState: IProjectsState = {
  data: null,
  loading: false,
};

export const updateProjectsReducer: Reducer<
  IProjectsState,
  IUpdateProjectAction
> = (state = projectsInitialState, action: IUpdateProjectAction) => {
  switch (action.type) {
    case updateProjectActionTypes.UPDATE_PROJECT_STATE: {
      return { ...state, data: [...action.payload] };
    }
    default: {
      return state;
    }
  }
};

export const fetchProjectsReducer: Reducer<
  IProjectsState,
  fetchProjectsAction
> = (state = projectsInitialState, action: fetchProjectsAction) => {
  switch (action.type) {
    case fetchProjectsActionTypes.FETCH_PROJECTS_REQUEST: {
      return { ...state, loading: true };
    }
    case fetchProjectsActionTypes.FETCH_PROJECTS_SUCCESS: {
      return { ...state, loading: false, data: action.payload };
    }
    case fetchProjectsActionTypes.FETCH_PROJECTS_FAILURE: {
      return { ...state };
    }
    default: {
      return state;
    }
  }
};

export const projectsReducer: Reducer<IProjectsState> = (
  state = projectsInitialState,
  action
) => {
  if (Object.values(updateProjectActionTypes).includes(action.type)) {
    return updateProjectsReducer(state, action as IUpdateProjectAction);
  }
  if (Object.values(fetchProjectsActionTypes).includes(action.type)) {
    return fetchProjectsReducer(state, action);
  }
  return state;
};
