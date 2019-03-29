import axios, { AxiosPromise, AxiosRequestConfig } from 'axios';

import { IDeployConfig, IDeployedStatusInfo, IDeployStatusInfo } from 'models/Deploy';
import { URL } from 'utils/types';

import { BaseDataService } from '../BaseDataService';
import { IDeployService } from '../IDeployService';
import { deployedStatusInfoData } from './deployMock';

const delay = (ms: number) => new Promise(res => setTimeout(res, ms));

let isDeployed = false;

export default class DeployService extends BaseDataService implements IDeployService {
  private deployStatusInfoByModels: Record<string, IDeployStatusInfo> = {};

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
      return { status: 'deployed', data: deployedStatusInfoData } as any;
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
