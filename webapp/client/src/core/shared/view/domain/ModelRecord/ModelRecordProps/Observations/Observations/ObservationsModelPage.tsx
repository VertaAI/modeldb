import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';

import { IObservation, Observation } from 'core/shared/models/Observation';
import { withScientificNotationOrRounded } from 'core/shared/utils/formatters/number';

import { mapToObject } from 'core/shared/utils/collection';
import { Icon } from 'core/shared/view/elements/Icon/Icon';
import ScrollableContainer from 'core/shared/view/elements/ScrollableContainer/ScrollableContainer';

import ClientSuggestion from '../../shared/ClientSuggestion/ClientSuggestion';
import ObservationButton from '../ObservationButton/ObservationButton';
import ObservationsChart from './charts/ObservationsChart';
import styles from './ObservationsModelPage.module.css';

export interface IObservationsValues {
  value: string | number;
  timeStamp: Date;
}

interface IObservationLineData {
  lineIndex: string;
  values: IObservationsValues[];
}

interface IObservationObject {
  [key: string]: IObservationsValues[];
}

interface ILocalProps {
  observations: IObservation[];
  getObservationClassname?: (key: string) => string | undefined;
  docLink?: string;
}
interface ILocalState {
  selectedObservations: Map<string, boolean>;
}

function getInitialSelection(groupedObs: Map<string, IObservationsValues[]>) {
  const observationAttrs = Array.from(groupedObs.keys());
  const initialList = observationAttrs.slice(0, 3);
  const selection: Map<string, boolean> = new Map();
  observationAttrs.forEach((key: string) => {
    selection.set(key, initialList.includes(key));
  });
  return selection;
}

class ObservationsModelPage extends React.PureComponent<
  ILocalProps,
  ILocalState
> {
  public state = {
    selectedObservations: getInitialSelection(
      this.groupObservations(this.props.observations)
    ),
  };
  public render() {
    const {
      observations,
      getObservationClassname = () => undefined,
      docLink,
    } = this.props;
    const groupedObs = this.groupObservations(observations);
    return (
      <div>
        {groupedObs.size > 0 ? (
          <div className={styles.observations_wrapper}>
            <ScrollableContainer
              maxHeight={480}
              containerOffsetValue={12}
              children={
                <>
                  {[...groupedObs.keys()].map(
                    (attributeKey: string, i: number) => {
                      const selectedObservations = this.state
                        .selectedObservations;
                      return (
                        <div className={styles.attribute_container}>
                          <ObservationButton
                            additionalClassname={getObservationClassname(
                              attributeKey
                            )}
                            groupedObs={groupedObs}
                            attributeKey={attributeKey}
                            key={i}
                          />
                          <Icon
                            type={'arrow-right-circle'}
                            className={cn(
                              styles.icon,
                              selectedObservations.get(attributeKey)
                                ? styles.selected_option_icon
                                : styles.option_icon
                            )}
                            dataId={attributeKey}
                            key={attributeKey}
                            onClick={this.handleSelection}
                          />
                        </div>
                      );
                    }
                  )}
                </>
              }
            />
            <div className={styles.observations_chart_container}>
              <ObservationsChart
                data={this.convertObservationShape(mapToObject(groupedObs))}
              />
            </div>
          </div>
        ) : docLink ? (
          <ClientSuggestion
            fieldName={'observation'}
            clientMethod={'log_observation()'}
            link={docLink}
          />
        ) : (
          ''
        )}
      </div>
    );
  }

  @bind
  private handleSelection(e: React.MouseEvent<any, MouseEvent>) {
    const key = e.currentTarget.dataset.id;
    const selection = new Map(this.state.selectedObservations);
    const currentSelection = selection.get(key);
    selection.set(key, !currentSelection);
    this.setState({
      selectedObservations: selection,
    });
  }

  @bind
  private convertObservationShape(obj: IObservationObject) {
    const listOfObj: IObservationLineData[] = [];
    const entries = Object.entries(obj);
    entries.forEach(([key, value]) => {
      if (this.state.selectedObservations.get(key)) {
        listOfObj.push({
          lineIndex: key,
          values: value,
        });
      }
    });
    return listOfObj;
  }

  @bind
  private groupObservations(observations: Observation[]) {
    const map: Map<string, IObservationsValues[]> = new Map();
    observations.forEach((obs: Observation) => {
      const key = obs.attribute.key;
      const collection = map.get(key);
      if (!collection) {
        if (obs.attribute.key) {
          map.set(key, [
            {
              value: withScientificNotationOrRounded(
                Number(obs.attribute.value)
              ),
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
  }
}

export default ObservationsModelPage;
