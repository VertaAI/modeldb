import { JsonObject, JsonProperty } from 'json2typescript';

import { AnyToStringOrNumberConverter } from 'core/shared/utils/mapperConverters';

import { IKeyValuePair } from './Common';

export type IMetric = IKeyValuePair<string | number>;

@JsonObject('metric')
export class Metric implements IMetric {
  @JsonProperty('key', String)
  public readonly key: string;
  @JsonProperty('value', AnyToStringOrNumberConverter)
  public readonly value: number | string;

  constructor(key: string, value: number | string) {
    this.key = key;
    this.value = value;
  }
}
