import { IDeployStatusInfo } from 'models/Deploy';

export interface IDeployState {
  data: IDeployStatusInfoByModelId;
  requestingToDeploy: Record<ModelID, { isRequesting: boolean; error: string }>;
  loadingDeployStatus: Record<ModelID, { isRequesting: boolean; error: string }>;
  // todo rename
  checkingDeployStatus: Record<ModelID, { isRequesting: boolean; error: string }>;
}

type ModelID = string;

export interface IDeployStatusInfoByModelId {
  [modelId: string]: IDeployStatusInfo;
}

// todo rename
export enum deployActionTypes {
  DEPLOY_REQUEST = '@@deploy/DEPLOY_REQUEST',
  DEPLOY_SUCCESS = '@@deploy/DEPLOY_SUCCESS',
  DEPLOY_FAILURE = '@@deploy/DEPLOY_FAILURE'
}
export type deployAction =
  | { type: deployActionTypes.DEPLOY_REQUEST; payload: ModelID }
  | { type: deployActionTypes.DEPLOY_SUCCESS; payload: ModelID }
  | { type: deployActionTypes.DEPLOY_FAILURE; payload: { modelId: ModelID; error: string } };

export enum loadDeployStatusActionTypes {
  LOAD_DEPLOY_STATUS_REQUEST = '@@deploy/LOAD_DEPLOY_STATUS_REQUEST',
  LOAD_DEPLOY_STATUS_SUCCESS = '@@deploy/LOAD_DEPLOY_STATUS_SUCCESS',
  LOAD_DEPLOY_STATUS_FAILURE = '@@deploy/LOAD_DEPLOY_STATUS_FAILURE'
}
export type loadDeployStatusAction =
  | { type: loadDeployStatusActionTypes.LOAD_DEPLOY_STATUS_REQUEST; payload: ModelID }
  | { type: loadDeployStatusActionTypes.LOAD_DEPLOY_STATUS_SUCCESS; payload: { modelId: ModelID; info: IDeployStatusInfo } }
  | { type: loadDeployStatusActionTypes.LOAD_DEPLOY_STATUS_FAILURE; payload: { modelId: ModelID; error: string } };

export enum checkDeployStatusActionTypes {
  CHECK_DEPLOY_STATUS_REQUEST = '@@deploy/CHECK_DEPLOY_STATUS_REQUEST',
  CHECK_DEPLOY_STATUS_SUCCESS = '@@deploy/CHECK_DEPLOY_STATUS_SUCCESS',
  CHECK_DEPLOY_STATUS_FAILURE = '@@deploy/CHECK_DEPLOY_STATUS_FAILURE'
}
export type checkDeployStatusAction =
  | { type: checkDeployStatusActionTypes.CHECK_DEPLOY_STATUS_REQUEST; payload: ModelID }
  | { type: checkDeployStatusActionTypes.CHECK_DEPLOY_STATUS_SUCCESS; payload: ModelID }
  | { type: checkDeployStatusActionTypes.CHECK_DEPLOY_STATUS_FAILURE; payload: { modelId: ModelID; error: string } };

export type allActions = deployAction | loadDeployStatusAction | checkDeployStatusAction;
