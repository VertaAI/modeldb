import { AxiosPromise } from 'axios';

import {
  IDataStatistics,
  IDeployStatusInfo,
  IServiceStatistics,
} from 'models/Deploy';

import { IDeployRequest } from './DeployService';

export interface IDeployService {
  deploy(req: IDeployRequest): AxiosPromise<void>;
  delete(modelId: string): AxiosPromise<void>;
  loadStatus(modelId: string): AxiosPromise<IDeployStatusInfo>;
  getServiceStatistics(modelId: string): AxiosPromise<IServiceStatistics>;
  getDataStatistics(modelId: string): AxiosPromise<IDataStatistics>;
}
