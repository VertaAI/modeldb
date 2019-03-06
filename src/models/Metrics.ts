import { JsonObject, JsonProperty } from 'json2typescript';

import { AnyToStringOrNumberConverter } from '../utils/MapperConverters';

export interface IMetric {
  key: string;
  value: number | string;
}

@JsonObject('metric')
export class Metric implements IMetric {
  @JsonProperty('key', String)
  public readonly key: string;
  @JsonProperty('value', AnyToStringOrNumberConverter)
  public readonly value: number | string;

  constructor(key?: string, value?: number | string) {
    this.key = key || '';
    this.value = value || '' || 0;
  }
}
