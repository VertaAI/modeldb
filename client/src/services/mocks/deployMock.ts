import { IDeployedStatusInfo } from 'models/Deploy';
import {
  IServerServiceData,
  IServerServiceStatistics,
} from 'services/serverModel/Deploy';

export const deployedStatusInfoData: IDeployedStatusInfo['data'] = {
  token: 'token',
  uptime: 34,
  type: 'rest',
  api: 'https://verta.io/234wfogsfas/fsfbgs',
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
        { name: 'city', type: 'float' },
      ],
    },
    output: {
      name: 'class1_prob',
      type: 'float',
    },
  },
};

export const mockDataStatistics: IServerServiceData = {
  feature1: {
    count: [10, 20, 30, 20, 10],
    bucket_limits: [11, 12, 13, 14, 15, 16],
    reference: [11, 5, 5, 5, 20],
  },
  feature2: {
    count: [140, 220, 330, 210, 160],
    bucket_limits: [110, 120, 130, 140, 150, 160],
    reference: [112, 53, 54, 52, 20],
  },
};

export const mockServiceStatistics: IServerServiceStatistics = {
  latency_avg: [0.16063440603015589, 0.16557458093694338, 0.16083773560703907],
  latency_p50: [0.08912037037037038, 0.08931818181818182, 0.08919491525423728],
  latency_p90: [0.5350000000000001, 0.5625, 0.5458333333333334],
  latency_p99: [2.027499999999998, 2.0124999999999957, 1.9825000000000017],
  throughput: [0.618181818181818, 0.618181818181818, 0.618181818181818],
  time: [1553757883, 1553757888, 1553757893],
};
