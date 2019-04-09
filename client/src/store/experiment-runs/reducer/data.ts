import { Reducer } from 'redux';

import {
  FeatureAction,
  IExperimentRunsState,
  loadExperimentRunsActionTypes,
} from '../types';

const initial: IExperimentRunsState['data'] = {
  modelRecords: null,
};

const dataReducer: Reducer<IExperimentRunsState['data'], FeatureAction> = (
  state = initial,
  action
) => {
  switch (action.type) {
    case loadExperimentRunsActionTypes.SUCCESS: {
      return { ...state, modelRecords: action.payload };
    }
    default:
      return state;
  }
};

export default dataReducer;
