import * as React from 'react';

import Icon from 'components/shared/Icon/Icon';

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
                  <Icon className={styles.notif_icon} type="database" />
                </div>
                <div className={styles.artifactKey}>{dataset.key}</div>
              </div>
            );
          })}
      </div>
    );
  }
}

export default DatasetsColDef;
