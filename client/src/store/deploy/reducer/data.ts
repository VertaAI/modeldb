import { combineReducers, Reducer } from 'redux';

import {
  deployActionTypes,
  FeatureAction,
  IDeployState,
  IToggleDeployManagerActions,
  loadDataStatisticsActionTypes,
  loadDeployStatusActionTypes,
  loadServiceStatisticsActionTypes,
  toggleDeployManagerActionTypes,
} from '../types';

const initial: IDeployState['data'] = {
  dataStatistics: null,
  shownDeployManagerModelId: null,
  deployStatusInfoByModelId: {},
  serviceStatistics: null,
};

const shownDeployManagerModelIdReducer: Reducer<
  IDeployState['data']['shownDeployManagerModelId'],
  IToggleDeployManagerActions
> = (state = initial.shownDeployManagerModelId, action) => {
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

const deployStatusInfoReducer: Reducer<
  IDeployState['data']['deployStatusInfoByModelId'],
  FeatureAction
> = (state = initial.deployStatusInfoByModelId, action) => {
  switch (action.type) {
    case deployActionTypes.REQUEST: {
      return { ...state, [action.payload]: { status: 'deploying' } };
    }
    case loadDeployStatusActionTypes.SUCCESS: {
      return { ...state, [action.payload.modelId]: action.payload.info };
    }
    default:
      return state;
  }
};

const serviceStatisticsReducer: Reducer<
  IDeployState['data']['serviceStatistics'],
  FeatureAction
> = (state = initial.serviceStatistics, action) => {
  switch (action.type) {
    case loadServiceStatisticsActionTypes.SUCCESS: {
      return action.payload;
    }
    default:
      return state;
  }
};

const dataStatisticsReducer: Reducer<
  IDeployState['data']['dataStatistics'],
  FeatureAction
> = (state = initial.dataStatistics, action) => {
  switch (action.type) {
    case loadDataStatisticsActionTypes.SUCCESS: {
      return action.payload;
    }
    default:
      return state;
  }
};

export default combineReducers<IDeployState['data']>({
  dataStatistics: dataStatisticsReducer,
  deployStatusInfoByModelId: deployStatusInfoReducer,
  serviceStatistics: serviceStatisticsReducer,
  shownDeployManagerModelId: shownDeployManagerModelIdReducer,
});
