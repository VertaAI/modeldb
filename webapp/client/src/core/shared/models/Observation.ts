import { JsonObject, JsonProperty } from 'json2typescript';

import { StringToDateConverter } from 'core/shared/utils/mapperConverters';

import { DataAttribute } from './DataAttribute';

export interface IObservation {
  attribute: DataAttribute;
  timestamp: Date;
}

@JsonObject('observation')
export class Observation implements IObservation {
  @JsonProperty('attribute', DataAttribute, true)
  public readonly attribute: DataAttribute;
  @JsonProperty('timestamp', StringToDateConverter, true)
  public readonly timestamp: Date;

  constructor(attribute?: DataAttribute, timestamp?: Date) {
    this.attribute = attribute || new DataAttribute();
    this.timestamp = timestamp || new Date();
  }
}
