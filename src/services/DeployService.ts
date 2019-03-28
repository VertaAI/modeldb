import axios, { AxiosPromise, AxiosRequestConfig } from 'axios';

import { IDeployConfig, IDeployStatusInfo, IDeployedStatusInfo } from 'models/Deploy';
import { URL } from 'utils/types';

import { BaseDataService } from './BaseDataService';
import { IDeployService } from './IDeployService';

const delay = (ms: number) => new Promise(res => setTimeout(res, ms));

let isDeployed = false;

const result: IDeployedStatusInfo['data'] = {
  token: 'token',
  uptime: 34,
  modelApi: {
    modelType: 'scikit',
    pythonVersion: 2,
    input: {
      type: 'list',
      fields: [
        { name: 'age', type: 'float' },
        { name: 'gender', type: 'float' },
        { name: 'zipcode', type: 'float' },
        { name: 'city', type: 'float' },
        { name: 'gender', type: 'float' },
        { name: 'zipcode', type: 'float' },
        { name: 'city', type: 'float' }
      ]
    },
    output: {
      name: 'class1_prob',
      type: 'float'
    }
  },
  type: 'rest',
  api: 'https://verta.io/234wfogsfas/fsfbgs'
};

export default class DeployService extends BaseDataService implements IDeployService {
  constructor() {
    super();
  }

  public async deploy(request: IDeployRequest): Promise<void> {
    await delay(100);
  }

  public async delete(modelId: string): Promise<void> {}

  public async loadStatus(modelId: string): Promise<IDeployStatusInfo> {
    if (!isDeployed) {
      setTimeout(() => {
        isDeployed = true;
      }, 600);
    }
    if (isDeployed) {
      return { status: 'deployed', data: result };
    }
    return { status: 'deploying' };
  }
}

export interface IDeployRequest {
  modelId: string;
  requirements: URL;
  model: URL;
  api: URL;
}
