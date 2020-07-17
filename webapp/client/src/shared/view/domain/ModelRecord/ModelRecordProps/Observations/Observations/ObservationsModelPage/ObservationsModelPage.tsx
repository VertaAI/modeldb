import cn from 'classnames';
import * as React from 'react';

import {
  IObservation,
  groupObservationsByAttributeKey,
  hasEpochValues,
} from 'shared/models/Observation';
import { Icon } from 'shared/view/elements/Icon/Icon';
import ScrollableContainer from 'shared/view/elements/ScrollableContainer/ScrollableContainer';
import Button from 'shared/view/elements/Button/Button';
import matchType from 'shared/utils/matchType';
import vertaDocLinks from 'shared/utils/globalConstants/vertaDocLinks';

import ClientSuggestion from '../../../shared/ClientSuggestion/ClientSuggestion';
import ObservationButton from '../../ObservationButton/ObservationButton';
import ObservationsChart from './ObservationsChart/ObservationsChart';
import styles from './ObservationsModelPage.module.css';
import { getObservationLineData } from './observationsChartHelpers';

interface ILocalProps {
  observations: IObservation[];
}

type XAxisType = 'epochNumber' | 'timeStamp';

const ObservationsModelPage = React.memo(({ observations }: ILocalProps) => {
  const [selectedObservations, setSelectedObservations] = React.useState<
    Record<string, boolean>
  >(() => getInitialSelection(observations));
  const toggleObservationSelection = (attributeKey: string) => {
    setSelectedObservations({
      ...selectedObservations,
      [attributeKey]: !selectedObservations[attributeKey],
    });
  };

  const { xAxisType, xAxisTypeSelect } = useXAxisTypeSelect(observations);

  const groupedObs = groupObservationsByAttributeKey(observations);

  return (
    <div>
      {Object.keys(groupedObs).length > 0 ? (
        <div>
          {xAxisTypeSelect}
          <div className={styles.observations_wrapper}>
            <ScrollableContainer
              maxHeight={480}
              containerOffsetValue={12}
              children={
                <>
                  {Object.entries(groupedObs).map(([attributeKey, values]) => {
                    return (
                      <div
                        className={styles.attribute_container}
                        key={attributeKey}
                      >
                        <ObservationButton
                          values={values}
                          attributeKey={attributeKey}
                        />
                        <Icon
                          type={'arrow-right-circle'}
                          className={cn(
                            styles.icon,
                            selectedObservations[attributeKey]
                              ? styles.selected_option_icon
                              : styles.option_icon
                          )}
                          key={attributeKey}
                          onClick={() =>
                            toggleObservationSelection(attributeKey)
                          }
                        />
                      </div>
                    );
                  })}
                </>
              }
            />
            <div className={styles.observations_chart_container}>
              <ObservationsChart
                data={getObservationLineData(groupedObs, selectedObservations)}
                xAxisType={xAxisType}
              />
            </div>
          </div>
        </div>
      ) : (
        <ClientSuggestion
          fieldName={'observation'}
          clientMethod={'log_observation()'}
          link={vertaDocLinks.log_observations}
        />
      )}
    </div>
  );
});

export const useXAxisTypeSelect = (observations: IObservation[]) => {
  const [xAxisType, setXAxisType] = React.useState<XAxisType>(() =>
    hasEpochValues(observations) ? 'epochNumber' : 'timeStamp'
  );

  const xAxisTypeSelect = hasEpochValues(observations) && (
    <div className={styles.changeXAxisButton}>
      <Button
        size="medium"
        fullWidth={true}
        onClick={() =>
          setXAxisType(
            matchType(
              {
                timeStamp: () => 'epochNumber',
                epochNumber: () => 'timeStamp',
              },
              xAxisType
            )
          )
        }
      >
        {matchType(
          {
            timeStamp: () => 'Switch to epoch',
            epochNumber: () => 'Switch to timestamp',
          },
          xAxisType
        )}
      </Button>
    </div>
  );

  return { xAxisType, setXAxisType, xAxisTypeSelect };
};

function getInitialSelection(observations: IObservation[]) {
  const observationAttrs = Object.keys(
    groupObservationsByAttributeKey(observations)
  );
  const initialSelectedObservationAttrs = observationAttrs.slice(0, 3);

  return Object.fromEntries(
    observationAttrs.map(
      attr => [attr, initialSelectedObservationAttrs.includes(attr)] as const
    )
  );
}

export default ObservationsModelPage;
