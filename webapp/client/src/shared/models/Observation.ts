import { JsonObject, JsonProperty } from 'json2typescript';

import { StringToDateConverter } from 'shared/utils/mapperConverters';

import { DataAttribute } from './DataAttribute';

export interface IObservation {
  attribute: DataAttribute;
  timestamp: Date;
  epochNumber?: number;
}

@JsonObject('observation')
export class Observation implements IObservation {
  @JsonProperty('attribute', DataAttribute, true)
  public readonly attribute: DataAttribute;
  @JsonProperty('timestamp', StringToDateConverter, true)
  public readonly timestamp: Date;
  @JsonProperty('epoch_number', Number, true)
  public readonly epochNumber?: number;

  constructor(
    attribute?: DataAttribute,
    timestamp?: Date,
    epochNumber?: number
  ) {
    this.attribute = attribute || new DataAttribute();
    this.timestamp = timestamp || new Date();
    this.epochNumber = epochNumber;
  }
}
