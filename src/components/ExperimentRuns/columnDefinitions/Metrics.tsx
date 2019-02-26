import * as React from 'react';
import styles from './ColumnDefs.module.css';

const ModelProperty: React.SFC<any> = props => {
  return (
    <div className={styles.param_grid}>
      <h3 className={styles.param_label}>{props.property.key}</h3>
      <p>{props.property.value}</p>
    </div>
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
