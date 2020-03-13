import { JsonObject, JsonProperty } from 'json2typescript';

import { AnyToStringOrNumberConverter } from 'core/shared/utils/mapperConverters';

import { IKeyValuePair } from './Common';

export type IAttribute = IKeyValuePair<
  string | number | Array<string | number>
>;

@JsonObject('Attribute')
export class Attribute implements IAttribute {
  @JsonProperty('key', String, true)
  public readonly key: string;
  @JsonProperty('value', AnyToStringOrNumberConverter)
  public readonly value: IAttribute['value'];

  constructor(key?: string, value?: IAttribute['value']) {
    this.key = key || '';
    this.value = value || '' || 0;
  }
}
