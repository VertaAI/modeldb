import axios, { AxiosPromise } from 'axios';

import {
  IDataStatistics,
  IDeployStatusInfo,
  IServiceStatistics,
} from 'models/Deploy';
import { URL } from 'utils/types';

import { BaseDataService } from './BaseDataService';
import {
  convertServerDataStatisticsToClient,
  convertServerServiceStatisticsToClient,
} from './converters/Deploy';
import { IDeployService } from './IDeployService';
import { deployedStatusInfoData } from './mocks/deployMock';

export class DeployService extends BaseDataService implements IDeployService {
  constructor() {
    super();
  }

  public getServiceStatistics(
    modelId: string
  ): AxiosPromise<IServiceStatistics> {
    return axios.get(`/v1/statistics/service/${modelId}/`, {
      transformResponse: convertServerServiceStatisticsToClient,
    });
  }

  public getDataStatistics(modelId: string): AxiosPromise<IDataStatistics> {
    return axios.get(`/v1/getDataStatistics/${modelId}/`, {
      transformResponse: convertServerDataStatisticsToClient,
    });
  }

  public deploy(request: IDeployRequest): AxiosPromise<void> {
    const serverRequest = {
      api: 's3://vertaai-deploymentservice-test/model_api.json',
      id: request.modelId,
      model: 's3://vertaai-deploymentservice-test/model.pkl',
      requirements: 's3://vertaai-deploymentservice-test/requirements.txt',
    };

    return axios.post('/v1/deployment/deploy', serverRequest);
  }

  public delete(modelId: string): AxiosPromise<void> {
    return axios.post('/v1/deployment/delete', { id: modelId });
  }

  public loadStatus(modelId: string): AxiosPromise<IDeployStatusInfo> {
    return axios.get<IDeployStatusInfo>(`/v1/deployment/status/${modelId}`, {
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
            data: { ...deployedStatusInfoData, api: res.api, token: res.token },
          } as IDeployStatusInfo;
        }
      },
    });
  }
}

export interface IDeployRequest {
  modelId: string;
  requirements: URL;
  model: URL;
  api: URL;
}
