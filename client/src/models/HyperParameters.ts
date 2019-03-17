import { JsonObject, JsonProperty } from 'json2typescript';

import { AnyToStringOrNumberConverter } from '../utils/MapperConverters';

export interface IHyperparameter {
  key: string;
  value: number | string;
}

@JsonObject('hyperparameter')
export class Hyperparameter implements IHyperparameter {
  @JsonProperty('key', String)
  public readonly key: string;
  @JsonProperty('value', AnyToStringOrNumberConverter)
  public readonly value: number | string;

  constructor(key?: string, value?: number | string) {
    this.key = key || '';
    this.value = value || '' || 0;
  }
}
