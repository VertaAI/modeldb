import * as React from 'react';
import { Link } from 'react-router-dom';
import styles from './ColumnDefs.module.css';
import { ComparisonType, PropertyType } from '../../../models/Filters';

import Draggable from '../../Draggable/Draggable';

class ModelRecordColDef extends React.Component<any> {
  public render() {
    const { id, projectId, experimentId } = this.props.data;
    return (
      <div className={styles.param_cell}>
        <Link className={styles.model_link} to={`/project/${projectId}/exp-run/${id}`}>
          <strong>Model ID</strong> {' : '}
          {`${id.slice(0, 4)}...${id.slice(-4)}`}
        </Link>
        <div className={styles.experiment_link}>{`Project ID: ${projectId.slice(0, 4)}...`}</div>
        <div className={styles.experiment_link}>{`Experiment ID: ${experimentId.slice(0, 4)}...`}</div>
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
