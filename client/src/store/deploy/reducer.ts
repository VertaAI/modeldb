import { combineReducers, Reducer } from 'redux';

import {
  allActions,
  checkDeployStatusAction,
  checkDeployStatusActionTypes,
  deployAction,
  deployActionTypes,
  fetchDataStatisticsActionTypes,
  fetchServiceStatisticsActionTypes,
  IDeployState,
  loadDeployStatusAction,
  loadDeployStatusActionTypes,
  toggleDeployManagerAction,
  toggleDeployManagerActionTypes,
} from './types';

const deployInitialState: IDeployState = {
  checkingDeployStatus: {},
  dataStatistics: null,
  deployStatusInfoByModelId: {},
  deploying: {},
  loadingDataStatistics: { isRequesting: false, error: '' },
  loadingDeployStatus: {},
  loadingServiceStatistics: { isRequesting: false, error: '' },
  serviceStatistics: null,
  shownDeployManagerModelId: null,
};

const deployManagerReducer: Reducer<
  IDeployState['shownDeployManagerModelId'],
  toggleDeployManagerAction
> = (state = deployInitialState.shownDeployManagerModelId, action) => {
  switch (action.type) {
    case toggleDeployManagerActionTypes.OPEN_DEPLOY_MANAGER: {
      return action.payload;
    }
    case toggleDeployManagerActionTypes.CLOSE_DEPLOY_MANAGER: {
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

const loadingServiceStatisticsReducer: Reducer<
  IDeployState['loadingServiceStatistics'],
  allActions
> = (state = deployInitialState.loadingServiceStatistics, action) => {
  switch (action.type) {
    case fetchServiceStatisticsActionTypes.FETCH_SERVICE_STATISTICS_REQUEST: {
      return { isRequesting: true, error: '' };
    }
    case fetchServiceStatisticsActionTypes.FETCH_SERVICE_STATISTICS_SUCCESS: {
      return { isRequesting: false, error: '' };
    }
    case fetchServiceStatisticsActionTypes.FETCH_SERVICE_STATISTICS_FAILURE: {
      return { isRequesting: false, error: action.payload };
    }
    default:
      return state;
  }
};

const serviceStatisticsReducer: Reducer<
  IDeployState['serviceStatistics'],
  allActions
> = (state = deployInitialState.serviceStatistics, action) => {
  switch (action.type) {
    case fetchServiceStatisticsActionTypes.FETCH_SERVICE_STATISTICS_SUCCESS: {
      return action.payload;
    }
    default:
      return state;
  }
};

const loadingDataStatisticsReducer: Reducer<
  IDeployState['loadingDataStatistics'],
  allActions
> = (state = deployInitialState.loadingServiceStatistics, action) => {
  switch (action.type) {
    case fetchDataStatisticsActionTypes.FETCH_DATA_STATISTICS_REQUEST: {
      return { isRequesting: true, error: '' };
    }
    case fetchDataStatisticsActionTypes.FETCH_DATA_STATISTICS_SUCCESS: {
      return { isRequesting: false, error: '' };
    }
    case fetchDataStatisticsActionTypes.FETCH_DATA_STATISTICS_FAILURE: {
      return { isRequesting: false, error: action.payload };
    }
    default:
      return state;
  }
};

const dataStatisticsReducer: Reducer<
  IDeployState['dataStatistics'],
  allActions
> = (state = deployInitialState.dataStatistics, action) => {
  switch (action.type) {
    case fetchDataStatisticsActionTypes.FETCH_DATA_STATISTICS_SUCCESS: {
      return action.payload;
    }
    default:
      return state;
  }
};

export const deployReducer = combineReducers<IDeployState>({
  checkingDeployStatus: checkingDeployStatusReducer,
  dataStatistics: dataStatisticsReducer,
  deployStatusInfoByModelId: deployStatusInfoReducer,
  deploying: deployingReducer,
  loadingDataStatistics: loadingDataStatisticsReducer,
  loadingDeployStatus: loadingDeployStatusReducer,
  loadingServiceStatistics: loadingServiceStatisticsReducer,
  serviceStatistics: serviceStatisticsReducer,
  shownDeployManagerModelId: deployManagerReducer,
});
