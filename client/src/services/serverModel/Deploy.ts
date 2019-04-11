export interface IServerServiceStatistics {
  latency_avg: number[];
  latency_p50: number[];
  latency_p90: number[];
  latency_p99: number[];
  throughput: number[];
  time: number[];
}

export interface IServerFeature {
  count: number[];
  bucket_limits: number[];
  reference: number[];
}

export interface IServerServiceData {
  [featureName: string]: IServerFeature;
}

export type IServerDeployStatusInfo =
  | { status: 'not deployed' }
  | { status: 'deploying' }
  | { status: 'live'; uptime: string; token: string; api: string };
