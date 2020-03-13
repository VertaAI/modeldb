import { JsonObject, JsonProperty } from 'json2typescript';

import { AnyToStringOrNumberConverter } from 'core/shared/utils/mapperConverters';

import { IKeyValuePair } from './Common';

export type IHyperparameter = IKeyValuePair<string | number>;

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
