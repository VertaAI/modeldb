import {
  IDataStatistics,
  IDeployStatusInfo,
  IServiceStatistics,
} from 'models/Deploy';

export interface IDeployState {
  shownDeployManagerModelId: ModelID | null;
  deployStatusInfoByModelId: IDeployStatusInfoByModelId;
  deploying: Record<ModelID, ICommunication>;
  loadingDeployStatus: Record<ModelID, ICommunication>;
  checkingDeployStatus: Record<ModelID, ICommunication>;
  loadingServiceStatistics: ICommunication;
  loadingDataStatistics: ICommunication;
  serviceStatistics: IServiceStatistics | null;
  dataStatistics: IDataStatistics | null;
}

interface ICommunication {
  isRequesting: boolean;
  error: string;
}

type ModelID = string;

export interface IDeployStatusInfoByModelId {
  [modelId: string]: IDeployStatusInfo;
}

export enum deployActionTypes {
  DEPLOY_REQUEST = '@@deploy/DEPLOY_REQUEST',
  DEPLOY_SUCCESS = '@@deploy/DEPLOY_SUCCESS',
  DEPLOY_FAILURE = '@@deploy/DEPLOY_FAILURE',
}
export type deployAction =
  | { type: deployActionTypes.DEPLOY_REQUEST; payload: ModelID }
  | { type: deployActionTypes.DEPLOY_SUCCESS; payload: ModelID }
  | {
      type: deployActionTypes.DEPLOY_FAILURE;
      payload: { modelId: ModelID; error: string };
    };

export enum loadDeployStatusActionTypes {
  LOAD_DEPLOY_STATUS_REQUEST = '@@deploy/LOAD_DEPLOY_STATUS_REQUEST',
  LOAD_DEPLOY_STATUS_SUCCESS = '@@deploy/LOAD_DEPLOY_STATUS_SUCCESS',
  LOAD_DEPLOY_STATUS_FAILURE = '@@deploy/LOAD_DEPLOY_STATUS_FAILURE',
}
export type loadDeployStatusAction =
  | {
      type: loadDeployStatusActionTypes.LOAD_DEPLOY_STATUS_REQUEST;
      payload: ModelID;
    }
  | {
      type: loadDeployStatusActionTypes.LOAD_DEPLOY_STATUS_SUCCESS;
      payload: { modelId: ModelID; info: IDeployStatusInfo };
    }
  | {
      type: loadDeployStatusActionTypes.LOAD_DEPLOY_STATUS_FAILURE;
      payload: { modelId: ModelID; error: string };
    };

export enum checkDeployStatusActionTypes {
  CHECK_DEPLOY_STATUS_REQUEST = '@@deploy/CHECK_DEPLOY_STATUS_REQUEST',
  CHECK_DEPLOY_STATUS_SUCCESS = '@@deploy/CHECK_DEPLOY_STATUS_SUCCESS',
  CHECK_DEPLOY_STATUS_FAILURE = '@@deploy/CHECK_DEPLOY_STATUS_FAILURE',
}
export type checkDeployStatusAction =
  | {
      type: checkDeployStatusActionTypes.CHECK_DEPLOY_STATUS_REQUEST;
      payload: ModelID;
    }
  | {
      type: checkDeployStatusActionTypes.CHECK_DEPLOY_STATUS_SUCCESS;
      payload: ModelID;
    }
  | {
      type: checkDeployStatusActionTypes.CHECK_DEPLOY_STATUS_FAILURE;
      payload: { modelId: ModelID; error: string };
    };

export enum toggleDeployManagerActionTypes {
  OPEN_DEPLOY_MANAGER = '@@deploy/OPEN_DEPLOY_MANAGER',
  CLOSE_DEPLOY_MANAGER = '@@deploy/CLOSE_DEPLOY_MANAGER',
}
export type toggleDeployManagerAction =
  | {
      type: toggleDeployManagerActionTypes.OPEN_DEPLOY_MANAGER;
      payload: ModelID;
    }
  | { type: toggleDeployManagerActionTypes.CLOSE_DEPLOY_MANAGER };

export enum fetchServiceStatisticsActionTypes {
  FETCH_SERVICE_STATISTICS_REQUEST = '@@deploy/FETCH_SERVICE_STATISTICS_REQUEST',
  FETCH_SERVICE_STATISTICS_SUCCESS = '@@deploy/FETCH_SERVICE_STATISTICS_SUCCESS',
  FETCH_SERVICE_STATISTICS_FAILURE = '@@deploy/FETCH_SERVICE_STATISTICS_FAILURE',
}
export type fetchServiceStatisticsAction =
  | {
      type: fetchServiceStatisticsActionTypes.FETCH_SERVICE_STATISTICS_REQUEST;
    }
  | {
      type: fetchServiceStatisticsActionTypes.FETCH_SERVICE_STATISTICS_SUCCESS;
      payload: IServiceStatistics;
    }
  | {
      type: fetchServiceStatisticsActionTypes.FETCH_SERVICE_STATISTICS_FAILURE;
      payload: string;
    };

export enum fetchDataStatisticsActionTypes {
  FETCH_DATA_STATISTICS_REQUEST = '@@deploy/FETCH_DATA_STATISTICS_REQUEST',
  FETCH_DATA_STATISTICS_SUCCESS = '@@deploy/FETCH_DATA_STATISTICS_SUCCESS',
  FETCH_DATA_STATISTICS_FAILURE = '@@deploy/FETCH_DATA_STATISTICS_FAILURE',
}
export type fetchDataStatisticsAction =
  | {
      type: fetchDataStatisticsActionTypes.FETCH_DATA_STATISTICS_REQUEST;
    }
  | {
      type: fetchDataStatisticsActionTypes.FETCH_DATA_STATISTICS_SUCCESS;
      payload: IDataStatistics;
    }
  | {
      type: fetchDataStatisticsActionTypes.FETCH_DATA_STATISTICS_FAILURE;
      payload: string;
    };

export type allActions =
  | deployAction
  | loadDeployStatusAction
  | checkDeployStatusAction
  | toggleDeployManagerAction
  | fetchServiceStatisticsAction
  | fetchDataStatisticsAction;
