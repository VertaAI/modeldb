import * as React from 'react';

import { IObservation, Observation } from 'core/shared/models/Observation';
import { withScientificNotationOrRounded } from 'core/shared/utils/formatters/number';
import ScrollableContainer from 'core/shared/view/elements/ScrollableContainer/ScrollableContainer';

import ClientSuggestion from '../../shared/ClientSuggestion/ClientSuggestion';
import ObservationButton from '../ObservationButton/ObservationButton';

interface ILocalProps {
  observations: IObservation[];
  getObservationClassname?: (key: string) => string | undefined;
  maxHeight?: number;
  docLink?: string;
}

class Observations extends React.PureComponent<ILocalProps> {
  public render() {
    const {
      observations,
      getObservationClassname = () => undefined,
      docLink,
      maxHeight,
    } = this.props;
    const groupedObs = groupObservations(observations);
    const observationKeys = [...groupedObs.keys()];
    return groupedObs.size !== 0 ? (
      <ScrollableContainer
        maxHeight={maxHeight || 180}
        containerOffsetValue={12}
        children={
          <>
            {observationKeys.map((attributeKey: string, i: number) => (
              <ObservationButton
                additionalClassname={getObservationClassname(attributeKey)}
                groupedObs={groupedObs}
                attributeKey={attributeKey}
                key={i}
              />
            ))}
          </>
        }
      />
    ) : docLink ? (
      <ClientSuggestion
        fieldName={'observation'}
        clientMethod={'log_observation()'}
        link={docLink}
      />
    ) : (
      ''
    );
  }
}

export interface IObservationsValues {
  value: string | number;
  timeStamp: Date;
}

export const groupObservations = (observations: Observation[]) => {
  const map: Map<string, IObservationsValues[]> = new Map();
  observations.forEach((obs: Observation) => {
    const key = obs.attribute.key;
    const collection = map.get(key);
    if (!collection) {
      if (obs.attribute.key) {
        map.set(key, [
          {
            value: withScientificNotationOrRounded(Number(obs.attribute.value)),
            timeStamp: obs.timestamp,
          },
        ]);
      }
    } else {
      if (obs.attribute.key) {
        collection.push({
          value: withScientificNotationOrRounded(Number(obs.attribute.value)),
          timeStamp: obs.timestamp,
        });
      }
    }
  });
  return map;
};

export default Observations;
