export type DeployType = 'rest' | 'batch';

export type ModelType = 'scikit' | 'xgboost';

export type DeployStatus = 'not-deployed' | 'building' | 'stopped' | 'running' | 'stopping' | 'error';

export type IDeployInfo = { status: 'not-deployed' } | { status: 'building' } | { status: 'running'; result: IDeployResult };

export interface IDeployConfig {
  type: DeployType;
  replicas: number;
  withLogs: boolean;
  withServiceMonitoring: boolean;
}

export interface IDeployResult {
  modelId: string;
  type: DeployType;
  url: string;
  modelApi: IModelApi;
}

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
