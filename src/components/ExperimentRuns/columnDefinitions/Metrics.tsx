import * as React from 'react';
import { ComparisonType, PropertyType } from '../../../models/Filters';
import Draggable from '../../Draggable/Draggable';
import styles from './ColumnDefs.module.css';

const ModelProperty: React.SFC<any> = props => {
  return (
    <Draggable
      additionalClassName={styles.param_draggable}
      type="filter"
      data={{ type: PropertyType.METRIC, name: props.property.key, value: props.property.value, comparisonType: ComparisonType.MORE }}
    >
      <div className={styles.param_grid_metric}>
        <div className={styles.param_key}>{props.property.key}</div>
        <div className={styles.param_val}>{props.property.value}</div>
      </div>
    </Draggable>
  );
};

class MetricsColDef extends React.Component<any> {
  public render() {
    const metricsObject = this.props.value;
    return (
      <div className={styles.param_cell}>
        {metricsObject.map((property: object, i: number) => {
          return <ModelProperty key={i} property={property} />;
        })}
      </div>
    );
  }
}

export default MetricsColDef;
