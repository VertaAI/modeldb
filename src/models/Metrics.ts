export interface IMetric {
  key: string;
  value: number | string;
}

export class Metric implements IMetric {
  public readonly key: string;
  public readonly value: number | string;
  constructor(key: string, value: number | string) {
    this.key = key;
    this.value = value;
  }
}
