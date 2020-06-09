import * as React from 'react';

import { makeDefaultMetricFilter } from 'core/shared/models/Filters';
import { IHyperparameter } from 'core/shared/models/HyperParameters';
import { IMetric } from 'core/shared/models/Metrics';
import { withScientificNotationOrRounded } from 'core/shared/utils/formatters/number';
import Draggable from 'core/shared/view/elements/Draggable/Draggable';
import ScrollableContainer from 'core/shared/view/elements/ScrollableContainer/ScrollableContainer';

import { IRow } from '../../types';
import styles from './SimpleKeyValuesColumn.module.css';

interface ILocalProps {
  row: IRow;
  type: 'metrics' | 'hyperparameters';
}

interface ILocalState {
  containerOffset: number;
}

type Entity = IMetric | IHyperparameter;

class SimpleKeyValuesColumn extends React.Component<ILocalProps, ILocalState> {
  public state = {
    containerOffset: 13,
  };

  public render() {
    const { row, type } = this.props;
    const entities = [...row.experimentRun[type]];
    return (
      <div className={styles.root}>
        <ScrollableContainer
          maxHeight={row.columnContentHeight || 180}
          containerOffsetValue={12}
          children={
            <div>
              {entities.map((entity, i) => {
                return (
                  <ModelPropertyDraggable key={i} type={type} entity={entity} />
                );
              })}
            </div>
          }
        />
      </div>
    );
  }
}

const ModelPropertyDraggable = ({
  entity,
  type,
}: {
  entity: Entity;
  type: ILocalProps['type'];
}) => {
  const formattedValue = (() => {
    if (typeof entity.value === 'number') {
      return withScientificNotationOrRounded(entity.value);
    }
    return entity.value;
  })();
  return (
    <Draggable
      additionalClassName={styles.param_draggable}
      type="filter"
      data={makeDefaultMetricFilter(`${type}.${entity.key}`, entity.value)}
    >
      <div
        className={styles[`param_grid_${type}`]}
        title={`Drag & Drop To Filter`}
        data-test={type === 'metrics' ? 'metric' : 'hyperparameter'}
      >
        <div
          className={styles.param_key}
          title={entity.key}
          data-test={type === 'metrics' ? 'metric-key' : 'hyperparameter-key'}
        >
          {entity.key}
        </div>
        <div
          className={styles.param_val}
          title={entity.key}
          data-test={
            type === 'metrics' ? 'metric-value' : 'hyperparameter-value'
          }
        >
          {formattedValue}
        </div>
      </div>
    </Draggable>
  );
};

export default SimpleKeyValuesColumn;
