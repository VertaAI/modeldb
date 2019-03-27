import axios, { AxiosPromise, AxiosRequestConfig } from 'axios';

import { IDeployConfig, IDeployResult } from 'models/Deploy';

import { BaseDataService } from './BaseDataService';

const delay = (ms: number) => new Promise(res => setTimeout(res, ms));

export default class DeployService extends BaseDataService {
  constructor() {
    super();
  }

  public async deploy(modelId: string, deployConfig: IDeployConfig): Promise<IDeployResult> {
    await delay(600);
    const result: IDeployResult = {
      modelId,
      modelApi: {
        modelType: 'scikit',
        pythonVersion: 2,
        input: {
          type: 'list',
          fields: [
            { name: 'age', type: 'float' },
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
      url: 'https://verta.io/234wfogsfas/fsfbgs'
    };
    return result;
  }
}
