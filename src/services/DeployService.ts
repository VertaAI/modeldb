import axios, { AxiosPromise, AxiosRequestConfig } from 'axios';

import { IDeployConfig, IDeployStatusInfo, IDeployedStatusInfo } from 'models/Deploy';
import { URL } from 'utils/types';

import { BaseDataService } from './BaseDataService';
import { IDeployService } from './IDeployService';

export class DeployService extends BaseDataService implements IDeployService {
  constructor() {
    super();
  }

  public deploy(request: IDeployRequest): AxiosPromise<void> {
    const serverRequest = {
      api: 's3://vertaai-deploymentservice-test/model_api.json',
      id: '1234',
      model: 's3://vertaai-deploymentservice-test/model.pkl',
      requirements: 's3://vertaai-deploymentservice-test/requirements.txt'
    };

    return axios.post('/api/v1/controller/deploy', serverRequest);
  }

  public delete(modelId: string): AxiosPromise<void> {
    return axios.post('/api/v1/controller/delete', { id: modelId });
  }

  public loadStatus(modelId: string): AxiosPromise<IDeployStatusInfo> {
    return axios.get<IDeployStatusInfo>(`/api/v1/controller/status/1234`, {
      transformResponse: res => {
        if (res.status === 'not deployed') {
          return { status: 'notDeployed' } as IDeployStatusInfo;
        }
        if (res.status === 'deploying') {
          return { status: 'deploying' } as IDeployStatusInfo;
        }
        if (res.status === 'live') {
          return {
            status: 'deployed',
            data: {
              api: res.api,
              token: res.token,
              uptime: 100,
              type: 'rest',
              modelApi: result.modelApi
            }
          } as IDeployStatusInfo;
        }
      }
    });
  }
}

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

export interface IDeployRequest {
  modelId: string;
  requirements: URL;
  model: URL;
  api: URL;
}
