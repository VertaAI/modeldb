import {
  IDataStatistics,
  IDeployStatusInfo,
  IServiceStatistics,
} from 'models/Deploy';
import {
  makeCommunicationActionTypes,
  MakeCommunicationActions,
  makeCommunicationReducerFromEnum,
  ICommunication,
} from 'utils/redux/communication';

export interface IDeployState {
  data: {
    shownDeployManagerModelId: ModelID | null;
    deployStatusInfoByModelId: IDeployStatusInfoByModelId;
    serviceStatistics: IServiceStatistics | null;
    dataStatistics: IDataStatistics | null;
  };
  communications: {
    deploying: Record<ModelID, ICommunication>;
    loadingDeployStatus: Record<ModelID, ICommunication>;
    checkingDeployStatus: Record<ModelID, ICommunication>;
    loadingDataStatistics: ICommunication;
    loadingServiceStatistics: ICommunication;
  };
}

type ModelID = string;

export interface IDeployStatusInfoByModelId {
  [modelId: string]: IDeployStatusInfo;
}

export const deployActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@deploy/DEPLOY_REQUEST',
  SUCCESS: '@@deploy/DEPLOY_SUCСESS',
  FAILURE: '@@deploy/DEPLOY_FAILURE',
});
export type IDeployActions = MakeCommunicationActions<
  typeof deployActionTypes,
  {
    request: ModelID;
    success: ModelID;
    failure: { modelId: ModelID; error: string };
  }
>;
export const deployReducer = makeCommunicationReducerFromEnum(
  deployActionTypes
);

export const loadDeployStatusActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@deploy/LOAD_DEPLOY_STATUS_REQUEST',
  SUCCESS: '@@deploy/LOAD_DEPLOY_STATUS_SUCСESS',
  FAILURE: '@@deploy/LOAD_DEPLOY_STATUS_FAILURE',
});
export type ILoadDeployStatusActions = MakeCommunicationActions<
  typeof loadDeployStatusActionTypes,
  {
    request: ModelID;
    success: { modelId: ModelID; info: IDeployStatusInfo };
    failure: { modelId: ModelID; error: string };
  }
>;
export const loadDeployStatusReducer = makeCommunicationReducerFromEnum(
  loadDeployStatusActionTypes
);

export const checkDeployStatusActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@deploy/CHECK_DEPLOY_STATUS_REQUEST',
  SUCCESS: '@@deploy/CHECK_DEPLOY_STATUS_SUCСESS',
  FAILURE: '@@deploy/CHECK_DEPLOY_STATUS_FAILURE',
});
export type ICheckDeployStatusActions = MakeCommunicationActions<
  typeof checkDeployStatusActionTypes,
  {
    request: ModelID;
    success: ModelID;
    failure: { modelId: ModelID; error: string };
  }
>;
export const checkDeployStatusReducer = makeCommunicationReducerFromEnum(
  checkDeployStatusActionTypes
);

export enum toggleDeployManagerActionTypes {
  OPEN_DEPLOY_MANAGER = '@@deploy/OPEN_DEPLOY_MANAGER',
  CLOSE_DEPLOY_MANAGER = '@@deploy/CLOSE_DEPLOY_MANAGER',
}
export type IToggleDeployManagerActions =
  | {
      type: toggleDeployManagerActionTypes.OPEN_DEPLOY_MANAGER;
      payload: ModelID;
    }
  | { type: toggleDeployManagerActionTypes.CLOSE_DEPLOY_MANAGER };

export const loadServiceStatisticsActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@deploy/LOAD_SERVICE_STATISTICS_REQUEST',
  SUCCESS: '@@deploy/LOAD_SERVICE_STATISTICS_SUCСESS',
  FAILURE: '@@deploy/LOAD_SERVICE_STATISTICS_FAILURE',
});
export type ILoadServiceStatisticsActions = MakeCommunicationActions<
  typeof loadServiceStatisticsActionTypes,
  { success: IServiceStatistics }
>;
export const loadServiceStatisticsReducer = makeCommunicationReducerFromEnum(
  loadServiceStatisticsActionTypes
);

export const loadDataStatisticsActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@deploy/LOAD_DATA_STATISTICS_REQUEST',
  SUCCESS: '@@deploy/LOAD_DATA_STATISTICS_SUCСESS',
  FAILURE: '@@deploy/LOAD_DATA_STATISTICS_FAILURE',
});
export type ILoadDataStatisticsActions = MakeCommunicationActions<
  typeof loadDataStatisticsActionTypes,
  { success: IDataStatistics }
>;

export type FeatureAction =
  | IDeployActions
  | ILoadDeployStatusActions
  | ICheckDeployStatusActions
  | IToggleDeployManagerActions
  | ILoadServiceStatisticsActions
  | ILoadDataStatisticsActions;
