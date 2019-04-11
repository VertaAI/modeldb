import axios from 'axios';
import MockAdapter from 'axios-mock-adapter';

import { URL } from 'utils/types';

import { DeployService } from '../DeployService';
import { IDeployService } from '../IDeployService';
import {
  deployedStatusInfoData,
  mockDataStatistics,
  mockServiceStatistics,
} from './deployMock';

const deployStatusInfoByModels: Record<string, any> = {};
export default class MockDeployService extends DeployService
  implements IDeployService {
  constructor() {
    super();
    const mock = new MockAdapter(axios);
    mock.onPost('/api/v1/controller/deploy').reply(config => {
      const modelId: string = JSON.parse(config.data).id;
      deployStatusInfoByModels[modelId] = { status: 'deploying' };
      setTimeout(() => {
        deployStatusInfoByModels[modelId] = {
          status: 'live',
          data: deployedStatusInfoData,
        };
      }, 600);
      return [200, {}];
    });

    mock.onGet(/\/api\/v1\/controller\/status\/.+/).reply(config => {
      const modelId = last(config.url!.split('/'));
      const deployStatus = deployStatusInfoByModels[modelId]
        ? deployStatusInfoByModels[modelId]
        : getRandomItem([
            { status: 'not deployed' },
            { status: 'live', data: deployedStatusInfoData },
            { status: 'deploying' },
          ]);

      if (deployStatus.status === 'deploying') {
        setTimeout(() => {
          deployStatusInfoByModels[modelId] = {
            status: 'live',
            data: deployedStatusInfoData,
          };
        }, 400);
      }

      return [200, deployStatus];
    });

    mock.onGet(/\/api\/v1\/getDataStatistics\/.+/).reply(config => {
      return [200, mockDataStatistics];
    });

    mock.onGet(/\/api\/v1\/getServiceStatistics\/.+/).reply(config => {
      return [200, mockServiceStatistics];
    });
  }
}

const getRandomItem = <T>(items: T[]): T => {
  return items[Math.floor(Math.random() * items.length)];
};

const last = <T>(arr: T[]): T => arr[arr.length - 1];

export interface IDeployRequest {
  modelId: string;
  requirements: URL;
  model: URL;
  api: URL;
}
