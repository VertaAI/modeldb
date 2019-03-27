import { Reducer } from 'redux';

import { deployAction, deployActionTypes, IDeployState } from './types';

const deployInitialState: IDeployState = {
  data: {},
  deploying: {}
};

export const deployReducer: Reducer<IDeployState, deployAction> = (state = deployInitialState, action) => {
  switch (action.type) {
    case deployActionTypes.DEPLOY_REQUEST: {
      return { ...state, deploying: { ...state.deploying, [action.payload]: { isRequesting: true, error: '' } } };
    }
    case deployActionTypes.DEPLOY_SUCCESS: {
      return {
        ...state,
        data: {
          [action.payload.modelId]: action.payload
        },
        deploying: { ...state.deploying, [action.payload.modelId]: { isRequesting: false, error: '' } }
      };
    }
    case deployActionTypes.DEPLOY_FAILURE: {
      return {
        ...state,
        deploying: { ...state.deploying, [action.payload.modelId]: { isRequesting: false, error: action.payload.error } }
      };
    }
    default: {
      return state;
    }
  }
};
