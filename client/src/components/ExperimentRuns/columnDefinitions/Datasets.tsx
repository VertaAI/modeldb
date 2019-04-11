import * as React from 'react';
import { Link } from 'react-router-dom';

import styles from './ColumnDefs.module.css';

class DatasetsColDef extends React.Component<any> {
  public render() {
    const datasets = this.props.value;
    return (
      <div>
        {datasets &&
          datasets.map((dataset: any, i: number) => {
            return (
              <div
                key={i}
                className={styles.dataset_wrapper}
                title="view Datasets"
              >
                <div className={styles.notif}>
                  <i className="fa fa-database" style={{ color: '#6863ff' }} />
                </div>
                <div className={styles.type}>
                  {`DATA`} &nbsp; &nbsp; &nbsp; ->{' '}
                </div>
                <div className={styles.key}>{dataset.key}</div>
                <div className={styles.path}>
                  <span className={styles.path_label}>PATH : &nbsp;</span>
                  {dataset.path}
                </div>
              </div>
            );
          })}
      </div>
    );
  }
}

export default DatasetsColDef;
