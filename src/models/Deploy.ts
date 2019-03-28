import { Timestamp, URL } from 'utils/types';

export type DeployType = 'rest' | 'batch';

export interface IDeployConfig {
  type: DeployType;
  replicas: number;
  withLogs: boolean;
  withServiceMonitoring: boolean;
}

export interface INotDeployedStatusInfo {
  status: 'notDeployed';
}
export interface IDeployingStatusInfo {
  status: 'deploying';
}
export interface IDeployedStatusInfo {
  status: 'deployed';
  data: { uptime: Timestamp; type: DeployType; token: string; api: URL; modelApi: IModelApi };
}
export type IDeployStatusInfo = INotDeployedStatusInfo | IDeployingStatusInfo | IDeployedStatusInfo;

export interface IModelApi {
  modelType: 'scikit' | 'xgboost';
  pythonVersion: 2 | 3;
  input: IModelApiInput;
  output: IOutputField;
}

export interface IModelApiInput {
  type: 'list';
  fields: IInputField[];
}

export interface IInputField {
  name: string;
  type: string;
}

export type IOutputField = IInputField;
