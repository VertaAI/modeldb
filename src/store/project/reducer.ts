import { Reducer } from 'redux';
import { IProjectState, ProjectActionTypes } from './types';

const initialState: IProjectState = {
  data: null,
  loading: false
};

const reducer: Reducer<IProjectState> = (state = initialState, action) => {
  switch (action.type) {
    case ProjectActionTypes.FETCH_PROJECTS: {
      return { ...state, loading: true };
    }
    default: {
      return state;
    }
  }
};

export { reducer as projectReducer };
