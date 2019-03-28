import axios, { AxiosPromise, AxiosRequestConfig } from 'axios';

import { IDeployConfig, IDeployedStatusInfo, IDeployStatusInfo } from 'models/Deploy';
import { URL } from 'utils/types';

import { BaseDataService } from '../BaseDataService';
import { IDeployService } from '../IDeployService';

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

  public deploy(request: IDeployRequest): AxiosPromise<void> {
    // delay(100).then();
    return {} as any;
  }

  public delete(modelId: string): AxiosPromise<void> {
    return {} as any;
  }

  public loadStatus(modelId: string): AxiosPromise<IDeployStatusInfo> {
    if (!isDeployed) {
      setTimeout(() => {
        isDeployed = true;
      }, 600);
    }
    if (isDeployed) {
      return { status: 'deployed', data: result } as any;
    }
    return { status: 'deploying' } as any;
  }
}

export interface IDeployRequest {
  modelId: string;
  requirements: URL;
  model: URL;
  api: URL;
}
