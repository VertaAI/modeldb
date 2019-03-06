import { JsonObject, JsonProperty } from 'json2typescript';
import { DataAttribute } from './DataAttribute';

import { StringToDateConverter } from '../utils/MapperConverters';

export interface IObservation {
  attribute: DataAttribute;
  timestamp: Date;
}

@JsonObject('observation')
export class Observation implements IObservation {
  @JsonProperty('attribute', DataAttribute)
  public readonly attribute: DataAttribute;
  @JsonProperty('timestamp', StringToDateConverter)
  public readonly timestamp: Date;

  constructor(attribute?: DataAttribute, timestamp?: Date) {
    this.attribute = attribute || new DataAttribute();
    this.timestamp = timestamp || new Date();
  }
}
