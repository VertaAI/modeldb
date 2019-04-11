import { combineReducers, Reducer } from 'redux';

import { makeCommunicationReducerFromEnum } from 'utils/redux/communication';

import {
  IDeployState,
  loadDataStatisticsActionTypes,
  loadServiceStatisticsActionTypes,
  checkDeployStatusActionTypes,
  deployActionTypes,
  IDeployActions,
} from '../types';

const deployingReducer: Reducer<
  IDeployState['communications']['deploying'],
  IDeployActions
> = (state = {}, action) => {
  switch (action.type) {
    case deployActionTypes.REQUEST: {
      return {
        ...state,
        [action.payload]: { isSuccess: false, isRequesting: true, error: '' },
      };
    }
    case deployActionTypes.SUCCESS: {
      return {
        ...state,
        [action.payload]: { isSuccess: true, isRequesting: false, error: '' },
      };
    }
    case deployActionTypes.FAILURE: {
      return {
        ...state,
        [action.payload.modelId]: {
          error: action.payload.error,
          isSuccess: false,
          isRequesting: false,
        },
      };
    }
    default:
      return state;
  }
};

const loadingDeployStatusReducer: Reducer<
  IDeployState['communications']['loadingDeployStatus'],
  IDeployActions
> = (state = {}, action) => {
  switch (action.type) {
    case deployActionTypes.REQUEST: {
      return {
        ...state,
        [action.payload]: { isSuccess: false, isRequesting: true, error: '' },
      };
    }
    case deployActionTypes.SUCCESS: {
      return {
        ...state,
        [action.payload]: { isSuccess: true, isRequesting: false, error: '' },
      };
    }
    case deployActionTypes.FAILURE: {
      return {
        ...state,
        [action.payload.modelId]: {
          error: action.payload.error,
          isSuccess: false,
          isRequesting: false,
        },
      };
    }
    default:
      return state;
  }
};

const checkingDeployStatusReducer: Reducer<
  IDeployState['communications']['checkingDeployStatus'],
  IDeployActions
> = (state = {}, action) => {
  switch (action.type) {
    case deployActionTypes.REQUEST: {
      return {
        ...state,
        [action.payload]: { isSuccess: false, isRequesting: true, error: '' },
      };
    }
    case deployActionTypes.SUCCESS: {
      return {
        ...state,
        [action.payload]: { isSuccess: true, isRequesting: false, error: '' },
      };
    }
    case deployActionTypes.FAILURE: {
      return {
        ...state,
        [action.payload.modelId]: {
          error: action.payload.error,
          isSuccess: false,
          isRequesting: false,
        },
      };
    }
    default:
      return state;
  }
};

export default combineReducers<IDeployState['communications']>({
  loadingDataStatistics: makeCommunicationReducerFromEnum(
    loadDataStatisticsActionTypes
  ),
  loadingServiceStatistics: makeCommunicationReducerFromEnum(
    loadServiceStatisticsActionTypes
  ),
  deploying: deployingReducer,
  checkingDeployStatus: checkingDeployStatusReducer,
  loadingDeployStatus: loadingDeployStatusReducer,
});
