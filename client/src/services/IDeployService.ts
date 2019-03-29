import { AxiosPromise } from 'axios';

import { IDeployStatusInfo } from 'models/Deploy';

import { IDeployRequest } from './DeployService';

export interface IDeployService {
  deploy(req: IDeployRequest): AxiosPromise<void>;
  delete(modelId: string): AxiosPromise<void>;
  loadStatus(modelId: string): AxiosPromise<IDeployStatusInfo>;
}
