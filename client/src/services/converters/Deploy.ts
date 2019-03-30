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
  const features: Array<Record<string, IServiceDataFeature>> = [];
  for (const feature of Object.keys(serverResponse)) {
    const dataFeature: IServiceDataFeature = {
      bucketLimits: serverResponse[feature].bucket_limits,
      count: serverResponse[feature].count,
      reference: serverResponse[feature].reference,
    };
    const record: Record<string, IServiceDataFeature> = {
      [feature]: dataFeature,
    };

    features.push(record);
  }
  return { features };
}
