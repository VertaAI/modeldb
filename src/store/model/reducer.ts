import { Model } from 'models/Model';
import { Reducer } from 'redux';
import { IModelsState, modelsActionTypes, modelsFetchAction } from './types';

const initialState: IModelsState = {
  data: null,
  loading: false
};

const reducer: Reducer<IModelsState> = (state = initialState, action: modelsFetchAction) => {
  switch (action.type) {
    case modelsActionTypes.FETCH_MODELS_REQUEST: {
      return { ...state, loading: true };
    }
    case modelsActionTypes.FETCH_MODELS_SUCESS: {
      return { ...state, data: action.payload };
    }
    case modelsActionTypes.FETCH_MODELS_FAILURE: {
      return { ...state };
    }
    default: {
      return state;
    }
  }
};

export { reducer as modelsReducer };
