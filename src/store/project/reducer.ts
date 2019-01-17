import { Reducer } from 'redux';
import { IProjectsState, ProjectActionTypes } from './types';

const initialState: IProjectsState = {
  data: null,
  loading: false
};

const reducer: Reducer<IProjectsState> = (state = initialState, action) => {
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

export { reducer as projectReducer };
