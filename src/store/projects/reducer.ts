import { Reducer } from 'redux';
import { fetchProjectsAction, fetchProjectsActionTypes, IProjectsState } from './types';

const projectsInitialState: IProjectsState = {
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
    case fetchProjectsActionTypes.FETCH_PROJECTS_FAILURE: {
      return { ...state };
    }
    default: {
      return state;
    }
  }
};
