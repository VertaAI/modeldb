import { Reducer } from 'redux';
import { fetchModelAction, fetchModelActionTypes, IModelState } from './types';

const modelInitialState: IModelState = {
  data: null,
  loading: false
};

export const modelReducer: Reducer<IModelState> = (state = modelInitialState, action: fetchModelAction) => {
  switch (action.type) {
    case fetchModelActionTypes.FETCH_MODEL_REQUEST: {
      return { ...state, loading: true };
    }
    case fetchModelActionTypes.FETCH_MODEL_SUCESS: {
      return { ...state, data: action.payload };
    }
    case fetchModelActionTypes.FETCH_MODEL_FAILURE: {
      return { ...state };
    }
    default: {
      return state;
    }
  }
};
