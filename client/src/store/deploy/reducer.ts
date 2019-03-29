import { Reducer, combineReducers } from 'redux';

import {
  IDeployState,
  loadDeployStatusAction,
  loadDeployStatusActionTypes,
  deployAction,
  deployActionTypes,
  allActions,
  checkDeployStatusAction,
  checkDeployStatusActionTypes,
  toggleWizardAction,
  toggleWizardActionTypes,
} from './types';

const deployInitialState: IDeployState = {
  showWizardForModel: null,
  data: {},
  requestingToDeploy: {},
  loadingDeployStatus: {},
  checkingDeployStatus: {},
};

const showWizardForModelReducer: Reducer<
  IDeployState['showWizardForModel'],
  toggleWizardAction
> = (state = deployInitialState.showWizardForModel, action) => {
  switch (action.type) {
    case toggleWizardActionTypes.OPEN_WIZARD: {
      return action.payload;
    }
    case toggleWizardActionTypes.CLOSE_WIZARD: {
      return null;
    }
    default:
      return state;
  }
};

const requestingToDeployReducer: Reducer<
  IDeployState['requestingToDeploy'],
  deployAction
> = (state = deployInitialState.requestingToDeploy, action) => {
  switch (action.type) {
    case deployActionTypes.DEPLOY_REQUEST: {
      return { ...state, [action.payload]: { isRequesting: true, error: '' } };
    }
    case deployActionTypes.DEPLOY_SUCCESS: {
      return { ...state, [action.payload]: { isRequesting: false, error: '' } };
    }
    case deployActionTypes.DEPLOY_FAILURE: {
      return {
        ...state,
        [action.payload.modelId]: {
          isRequesting: false,
          error: action.payload.error,
        },
      };
    }
    default:
      return state;
  }
};

const loadingDeployStatusReducer: Reducer<
  IDeployState['loadingDeployStatus'],
  loadDeployStatusAction
> = (state = deployInitialState.loadingDeployStatus, action) => {
  switch (action.type) {
    case loadDeployStatusActionTypes.LOAD_DEPLOY_STATUS_REQUEST: {
      return { ...state, [action.payload]: { isRequesting: true, error: '' } };
    }
    case loadDeployStatusActionTypes.LOAD_DEPLOY_STATUS_SUCCESS: {
      return {
        ...state,
        [action.payload.modelId]: { isRequesting: false, error: '' },
      };
    }
    case loadDeployStatusActionTypes.LOAD_DEPLOY_STATUS_FAILURE: {
      return {
        ...state,
        [action.payload.modelId]: {
          isRequesting: false,
          error: action.payload.error,
        },
      };
    }
    default:
      return state;
  }
};

const checkingDeployStatusReducer: Reducer<
  IDeployState['checkingDeployStatus'],
  checkDeployStatusAction
> = (state = deployInitialState.loadingDeployStatus, action) => {
  switch (action.type) {
    case checkDeployStatusActionTypes.CHECK_DEPLOY_STATUS_REQUEST: {
      return { ...state, [action.payload]: { isRequesting: true, error: '' } };
    }
    case checkDeployStatusActionTypes.CHECK_DEPLOY_STATUS_SUCCESS: {
      return { ...state, [action.payload]: { isRequesting: false, error: '' } };
    }
    case checkDeployStatusActionTypes.CHECK_DEPLOY_STATUS_FAILURE: {
      return {
        ...state,
        [action.payload.modelId]: {
          isRequesting: false,
          error: action.payload.error,
        },
      };
    }
    default:
      return state;
  }
};

const deployStatusInfoReducer: Reducer<IDeployState['data'], allActions> = (
  state = deployInitialState.data,
  action
) => {
  switch (action.type) {
    case deployActionTypes.DEPLOY_SUCCESS: {
      return { ...state, [action.payload]: { status: 'deploying' } };
    }
    case loadDeployStatusActionTypes.LOAD_DEPLOY_STATUS_SUCCESS: {
      return { ...state, [action.payload.modelId]: action.payload.info };
    }
    default:
      return state;
  }
};

export const deployReducer = combineReducers<IDeployState>({
  showWizardForModel: showWizardForModelReducer,
  checkingDeployStatus: checkingDeployStatusReducer,
  data: deployStatusInfoReducer,
  loadingDeployStatus: loadingDeployStatusReducer,
  requestingToDeploy: requestingToDeployReducer,
});
