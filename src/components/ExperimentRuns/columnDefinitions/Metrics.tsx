import * as React from 'react';
import { ComparisonType, PropertyType } from '../../../models/Filters';
import Draggable from '../../Draggable/Draggable';
import styles from './ColumnDefs.module.css';

const ModelProperty: React.SFC<any> = props => {
  return (
    <Draggable
      additionalClassName={styles.param_grid}
      type="filter"
      data={{ type: PropertyType.METRIC, name: props.property.key, value: props.property.value, comparisonType: ComparisonType.MORE }}
    >
      <h3 className={styles.param_label}>{props.property.key}</h3>
      <p>{props.property.value}</p>
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
