import { Reducer } from 'redux';
import {
  fetchProjectsAction,
  fetchProjectsActionTypes,
  IProjectsState,
  IProjectState,
  projectFetchModelsAction,
  projectFetchModelsActionTypes
} from './types';

const projectsInitialState: IProjectsState = {
  data: null,
  loading: false
};

const projectInitialState: IProjectState = {
  data: null,
  loading: false
};

export const projectsReducer: Reducer<IProjectsState> = (state = projectsInitialState, action: fetchProjectsAction) => {
  switch (action.type) {
    case fetchProjectsActionTypes.FETCH_PROJECTS_REQUEST: {
      return { ...state, loading: true };
    }
    case fetchProjectsActionTypes.FETCH_PROJECTS_SUCESS: {
      return { ...state, loading: false, data: action.payload };
    }
    default: {
      return state;
    }
  }
};

export const projectReducer: Reducer<IProjectState> = (state = projectInitialState, action: projectFetchModelsAction) => {
  switch (action.type) {
    case projectFetchModelsActionTypes.FETCH_MODELS_REQUEST: {
      return { ...state, loading: true };
    }
    case projectFetchModelsActionTypes.FETCH_MODELS_SUCESS: {
      return { ...state, data: action.payload };
    }
    case projectFetchModelsActionTypes.FETCH_MODELS_FAILURE: {
      return { ...state };
    }
    default: {
      return state;
    }
  }
};
