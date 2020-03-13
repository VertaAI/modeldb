import { JsonObject, JsonProperty } from 'json2typescript';

import { AnyToStringOrNumberConverter } from 'core/shared/utils/mapperConverters';

export interface IDataAttribute {
  key: string;
  value: number | string;
}

@JsonObject('attribute')
export class DataAttribute implements IDataAttribute {
  @JsonProperty('key', String)
  public readonly key: string;
  @JsonProperty('value', AnyToStringOrNumberConverter)
  public readonly value: number | string;

  constructor(key?: string, value?: number | string) {
    this.key = key || '';
    this.value = value || '' || 0;
  }
}
