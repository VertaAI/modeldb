import { JsonObject, JsonProperty } from 'json2typescript';
import * as R from 'ramda';

import { StringToDateConverter } from 'shared/utils/mapperConverters';

import { DataAttribute } from './DataAttribute';
import { withScientificNotationOrRounded } from 'shared/utils/formatters/number';

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

export interface IObservationsValues {
  value: string | number;
  timeStamp: Date;
  epochNumber?: number;
}
export type IGroupedObservationsByAttributeKey = Record<
  string,
  IObservationsValues[]
>;
export const groupObservationsByAttributeKey = (
  observations: Observation[]
): IGroupedObservationsByAttributeKey => {
  return R.fromPairs(
    R.toPairs(R.groupBy(obs => obs.attribute.key, observations)).map(
      ([key, observations]) => [
        key,
        observations.map(observation => {
          const res: IObservationsValues = {
            timeStamp: observation.timestamp,
            value:
              typeof observation.attribute.value === 'number'
                ? withScientificNotationOrRounded(
                    Number(observation.attribute.value)
                  )
                : observation.attribute.value,
            epochNumber: observation.epochNumber,
          };
          return res;
        }),
      ]
    )
  );
};

export const hasEpochValues = (observations: IObservation[]): boolean => {
  return observations.some(
    ({ epochNumber }) => typeof epochNumber !== 'undefined'
  );
};
