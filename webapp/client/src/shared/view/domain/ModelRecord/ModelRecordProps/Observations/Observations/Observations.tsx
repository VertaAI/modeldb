import * as React from 'react';

import {
  IObservation,
  groupObservationsByAttributeKey,
} from 'shared/models/Observation';
import ScrollableContainer from 'shared/view/elements/ScrollableContainer/ScrollableContainer';

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

    const groupedObs = groupObservationsByAttributeKey(observations);
    return Object.keys(groupedObs).length !== 0 ? (
      <ScrollableContainer
        maxHeight={maxHeight || 180}
        containerOffsetValue={12}
        children={
          <>
            {Object.entries(groupedObs).map(([attributeKey, values]) => (
              <ObservationButton
                attributeKey={attributeKey}
                values={values}
                additionalClassname={getObservationClassname(attributeKey)}
                key={attributeKey}
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

export default Observations;
