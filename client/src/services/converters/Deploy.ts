import {
  IDataStatistics,
  IDeployStatusInfo,
  IServiceDataFeature,
  IServiceStatistics,
} from 'models/Deploy';
import { deployedStatusInfoData } from 'services/mocks/deployMock';
import {
  IServerDeployStatusInfo,
  IServerServiceData,
  IServerServiceStatistics,
} from 'services/serverModel/Deploy';

export function convertServerServiceStatisticsToClient(
  serverResponse: IServerServiceStatistics
): IServiceStatistics {
  return {
    averageLatency: serverResponse.latency_avg,
    p50Latency: serverResponse.latency_p50,
    p90Latency: serverResponse.latency_p90,
    p99Latency: serverResponse.latency_p99,
    throughput: serverResponse.throughput,
    time: serverResponse.time,
  };
}

export function convertServerDataStatisticsToClient(
  serverResponse: IServerServiceData
): IDataStatistics {
  const features: Map<string, IServiceDataFeature> = new Map<
    string,
    IServiceDataFeature
  >(
    Object.entries(serverResponse).map(
      ([feature, data]) =>
        [
          feature,
          {
            bucketLimits: data.bucket_limits,
            count: data.count,
            reference: data.reference,
          },
        ] as [string, IServiceDataFeature]
    )
  );

  return features;
}

export function convertServerDeployStatusInfoToClient(
  serverResponse: IServerDeployStatusInfo
): IDeployStatusInfo {
  switch (serverResponse.status) {
    case 'not deployed': {
      return { status: 'notDeployed' };
    }
    case 'deploying': {
      return { status: 'deploying' };
    }
    case 'live': {
      return {
        status: 'deployed',
        data: {
          ...deployedStatusInfoData,
          api: serverResponse.api,
          token: serverResponse.token,
        },
      };
    }
  }
}
