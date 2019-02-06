export interface IMetric {
  key: MetricKey;
  value: string;
}

export enum MetricKey {
  accuracy = 'accuracy'
}

export class Metric implements IMetric {
  public readonly key: MetricKey;
  public readonly value: string;
  constructor(key: MetricKey, value: string) {
    this.key = key;
    this.value = value;
  }
}
