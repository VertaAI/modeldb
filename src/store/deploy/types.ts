import { IDeployResult } from 'models/Deploy';

export interface IDeployState {
  data: IDeployResultByModelId;
  deploying: Record<string, { isRequesting: boolean; error: string }>;
}

export interface IDeployResultByModelId {
  [modelId: string]: IDeployResult;
}

export enum deployActionTypes {
  DEPLOY_REQUEST = '@@deploy/DEPLOY_REQUEST',
  DEPLOY_SUCCESS = '@@deploy/DEPLOY_SUCCESS',
  DEPLOY_FAILURE = '@@deploy/DEPLOY_FAILURE'
}
export type deployAction =
  | { type: deployActionTypes.DEPLOY_REQUEST; payload: string }
  | { type: deployActionTypes.DEPLOY_SUCCESS; payload: IDeployResult }
  | { type: deployActionTypes.DEPLOY_FAILURE; payload: { modelId: string; error: string } };
