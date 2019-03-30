import { Reducer } from 'redux';

import {
  fetchModelRecordAction,
  fetchModelRecordActionTypes,
  IModelRecordState,
} from './types';

const modelInitialState: IModelRecordState = {
  data: null,
  loading: false,
};

export const modelRecordReducer: Reducer<IModelRecordState> = (
  state = modelInitialState,
  action: fetchModelRecordAction
) => {
  switch (action.type) {
    case fetchModelRecordActionTypes.FETCH_MODEL_RECORD_REQUEST: {
      return { ...state, loading: true };
    }
    case fetchModelRecordActionTypes.FETCH_MODEL_RECORD_SUCCESS: {
      return { ...state, loading: false, data: action.payload || mock };
    }
    case fetchModelRecordActionTypes.FETCH_MODEL_RECORD_FAILURE: {
      return { ...state };
    }
    default: {
      return state;
    }
  }
};
