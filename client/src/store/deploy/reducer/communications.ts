import { combineReducers, Reducer } from 'redux';

import { makeCommunicationReducerFromEnum } from 'utils/redux/communication';

import {
  checkDeployStatusActionTypes,
  deleteActionTypes,
  deployActionTypes,
  ICheckDeployStatusActions,
  IDeployActions,
  IDeployState,
  ILoadDeployStatusActions,
  loadDataStatisticsActionTypes,
  loadDeployStatusActionTypes,
  loadServiceStatisticsActionTypes,
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
  ILoadDeployStatusActions
> = (state = {}, action) => {
  switch (action.type) {
    case loadDeployStatusActionTypes.REQUEST: {
      return {
        ...state,
        [action.payload]: { isSuccess: false, isRequesting: true, error: '' },
      };
    }
    case loadDeployStatusActionTypes.SUCCESS: {
      return {
        ...state,
        [action.payload.modelId]: {
          isSuccess: true,
          isRequesting: false,
          error: '',
        },
      };
    }
    case loadDeployStatusActionTypes.FAILURE: {
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
  ICheckDeployStatusActions
> = (state = {}, action) => {
  switch (action.type) {
    case checkDeployStatusActionTypes.REQUEST: {
      return {
        ...state,
        [action.payload]: { isSuccess: false, isRequesting: true, error: '' },
      };
    }
    case checkDeployStatusActionTypes.SUCCESS: {
      return {
        ...state,
        [action.payload]: { isSuccess: true, isRequesting: false, error: '' },
      };
    }
    case checkDeployStatusActionTypes.FAILURE: {
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
  deleting: makeCommunicationReducerFromEnum(deleteActionTypes),
  deploying: deployingReducer,
  checkingDeployStatus: checkingDeployStatusReducer,
  loadingDeployStatus: loadingDeployStatusReducer,
});
