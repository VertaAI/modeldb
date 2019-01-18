import { Reducer } from 'redux';
import { IProjectsState, IProjectState, ProjectActionTypes, projectFetchModelsAction, projectFetchModelsActionTypes } from './types';

const projectsInitialState: IProjectsState = {
  data: null,
  loading: false
};

const projectInitialState: IProjectState = {
  data: null,
  loading: false
};

export const projectsReducer: Reducer<IProjectsState> = (state = projectsInitialState, action) => {
  switch (action.type) {
    case ProjectActionTypes.FETCH_PROJECTS: {
      return { ...state, loading: true };
    }
    case ProjectActionTypes.FETCH_SUCCESS: {
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
