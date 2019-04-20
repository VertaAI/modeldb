import axios, { AxiosPromise } from 'axios';

import {
  IDataStatistics,
  IDeployStatusInfo,
  IServiceStatistics,
} from 'models/Deploy';
import { URL } from 'utils/types';

import { BaseDataService } from '../BaseDataService';
import {
  convertServerDataStatisticsToClient,
  convertServerDeployStatusInfoToClient,
  convertServerServiceStatisticsToClient,
} from '../converters/Deploy';
import { IDeployService } from './IDeployService';

export default class DeployService extends BaseDataService
  implements IDeployService {
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
    return axios.get(`/v1/statistics/data/${modelId}/`, {
      transformResponse: convertServerDataStatisticsToClient,
    });
  }

  public deploy(request: IDeployRequest): AxiosPromise<void> {
    const serverRequest = {
      id: request.modelId,
    };

    return axios.post('/v1/deployment/deploy', serverRequest);
  }

  public delete(modelId: string): AxiosPromise<void> {
    return axios.post('/v1/deployment/delete', { id: modelId });
  }

  public loadStatus(modelId: string): AxiosPromise<IDeployStatusInfo> {
    return axios.get<IDeployStatusInfo>(`/v1/deployment/status/${modelId}`, {
      transformResponse: convertServerDeployStatusInfoToClient,
    });
  }
}

export interface IDeployRequest {
  modelId: string;
  requirements: URL;
  model: URL;
  api: URL;
}
