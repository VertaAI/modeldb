export interface IMetric {
  key: string;
  value: string;
}

export class Metric implements IMetric {
  public readonly key: string;
  public readonly value: string;
  constructor(key: string, value: string) {
    this.key = key;
    this.value = value;
  }
}
