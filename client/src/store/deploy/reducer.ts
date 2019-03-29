import { combineReducers, Reducer } from 'redux';

import {
  allActions,
  checkDeployStatusAction,
  checkDeployStatusActionTypes,
  deployAction,
  deployActionTypes,
  IDeployState,
  loadDeployStatusAction,
  loadDeployStatusActionTypes,
  toggleWizardAction,
  toggleWizardActionTypes,
} from './types';

const deployInitialState: IDeployState = {
  checkingDeployStatus: {},
  deployStatusInfoByModelId: {},
  deploying: {},
  loadingDeployStatus: {},
  showWizardForModel: null,
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

const deployingReducer: Reducer<IDeployState['deploying'], deployAction> = (
  state = deployInitialState.deploying,
  action
) => {
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
          error: action.payload.error,
          isRequesting: false,
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
          error: action.payload.error,
          isRequesting: false,
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
          error: action.payload.error,
          isRequesting: false,
        },
      };
    }
    default:
      return state;
  }
};

const deployStatusInfoReducer: Reducer<
  IDeployState['deployStatusInfoByModelId'],
  allActions
> = (state = deployInitialState.deployStatusInfoByModelId, action) => {
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
  checkingDeployStatus: checkingDeployStatusReducer,
  deployStatusInfoByModelId: deployStatusInfoReducer,
  deploying: deployingReducer,
  loadingDeployStatus: loadingDeployStatusReducer,
  showWizardForModel: showWizardForModelReducer,
});
