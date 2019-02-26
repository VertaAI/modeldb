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

class HyperparamsColDef extends React.Component<any> {
  public render() {
    const hyperparamObject = this.props.value;
    return (
      <div className={styles.param_cell}>
        {hyperparamObject.map((property: object, i: number) => {
          return <ModelProperty key={i} property={property} />;
        })}
      </div>
    );
  }
}

export default HyperparamsColDef;
