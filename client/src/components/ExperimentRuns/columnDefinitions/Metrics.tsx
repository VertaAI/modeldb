import * as React from 'react';

import Draggable from 'components/shared/Draggable/Draggable';
import { ComparisonType, PropertyType } from 'models/Filters';
import { numberTo4Decimal } from 'utils/MapperConverters/NumberFormatter';

import styles from './ColumnDefs.module.css';

const ModelProperty: React.SFC<any> = props => {
  let adjustedVal = numberTo4Decimal(props.property.value).toString();
  if (adjustedVal == '0') adjustedVal = props.property.value.toExponential();
  return (
    <Draggable
      additionalClassName={styles.param_draggable}
      type="filter"
      data={{
        type: PropertyType.METRIC,
        name: props.property.key,
        value: props.property.value,
        comparisonType: ComparisonType.MORE,
      }}
    >
      <div className={styles.param_grid_metric}>
        <div className={styles.param_key}>{props.property.key}</div>
        <div className={styles.param_val}>
          {typeof props.property.value === 'number'
            ? numberTo4Decimal(props.property.value)
            : props.property.value}
        </div>
      </div>
    </Draggable>
  );
};

class MetricsColDef extends React.Component<any> {
  public render() {
    const metricsObject = this.props.value;
    return (
      <div className={styles.param_cell} title={`Drag & Drop To Filter`}>
        {metricsObject.map((property: object, i: number) => {
          return <ModelProperty key={i} property={property} />;
        })}
      </div>
    );
  }
}

export default MetricsColDef;
