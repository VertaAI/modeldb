import {
  IDataStatistics,
  IServiceDataFeature,
  IServiceStatistics,
} from 'models/Deploy';
import {
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
  const features: Array<Record<string, IServiceDataFeature>> = Object.entries(
    serverResponse
  ).map(([feature, data]) => ({
    [feature]: {
      bucketLimits: data.bucket_limits,
      count: data.count,
      reference: data.reference,
    },
  }));

  return { features };
}
