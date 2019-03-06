import * as React from 'react';
import { Link } from 'react-router-dom';
import styles from './ColumnDefs.module.css';
import { ComparisonType, PropertyType } from '../../../models/Filters';

import Draggable from '../../Draggable/Draggable';

class ModelRecordColDef extends React.Component<any> {
  public render() {
    const modelRecord = this.props.data;
    return (
      <div className={styles.param_cell}>
        <Link className={styles.model_link} to={`/project/${modelRecord.projectId}/exp-run/${modelRecord.id}`}>
          <strong>Model ID</strong>
        </Link>
        <div className={styles.experiment_link}>Project ID</div>
        <div className={styles.experiment_link}>Experiment ID</div>
        {/* <Draggable
          type="filter"
          data={{ type: PropertyType.METRIC, name: 'expID', value: modelRecord.expId, comparisonType: ComparisonType.EQUALS }}
        >
          <div className={styles.experiment_link}>Experiment ID</div>
        </Draggable> */}
      </div>
    );
  }
}

export default ModelRecordColDef;
