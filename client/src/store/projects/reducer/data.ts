import { Reducer } from 'redux';

import {
  FeatureAction,
  IProjectsState,
  loadProjectsActionTypes,
  updateProjectActionTypes,
} from '../types';

const initial: IProjectsState['data'] = {
  projects: null,
};

const dataReducer: Reducer<IProjectsState['data'], FeatureAction> = (
  state = initial,
  action
) => {
  switch (action.type) {
    case loadProjectsActionTypes.SUCCESS: {
      return { ...state, projects: action.payload };
    }
    case updateProjectActionTypes.UPDATE_PROJECT_STATE: {
      return { ...state, projects: [...action.payload] };
    }
    default:
      return state;
  }
};

export default dataReducer;
