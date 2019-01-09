import {Reducer} from 'redux';
import {IProjectState, projectActionTypes} from './types';

const initialState: IProjectState = {
  data: null,
  loading: false,
};

const reducer: Reducer<IProjectState> = (state = initialState, action) => {
  switch (action.type) {
    case projectActionTypes.FETCH_PROJECTS: {
      return { ...state, loading: true };
    }
    default: {
      return state;
    }
  }
};

export {reducer as projectReducer};
