import { Reducer } from 'redux';

import {
  FeatureAction,
  IModelRecordState,
  loadModelRecordActionTypes,
} from '../types';

const initial: IModelRecordState['data'] = {
  modelRecord: null,
};

const dataReducer: Reducer<IModelRecordState['data'], FeatureAction> = (
  state = initial,
  action
) => {
  switch (action.type) {
    case loadModelRecordActionTypes.success: {
      return { ...state, modelRecord: action.payload };
    }
    default:
      return state;
  }
};

export default dataReducer;
