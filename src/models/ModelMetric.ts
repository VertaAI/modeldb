export interface IModelMetric {
  key: MetricKey;
  value: string;
  valueType: ValueType;
}

export enum MetricKey {
  Accuracy = 'accuracy'
}

export enum ValueType {
  Number = 'NUMBER'
}

export class ModelMetric implements IModelMetric {
  public readonly key: MetricKey;
  public readonly value: string;
  public readonly valueType: ValueType;
  constructor(key: MetricKey, value: string, valueType: ValueType) {
    this.key = key;
    this.value = value;
    this.valueType = valueType;
  }
}
