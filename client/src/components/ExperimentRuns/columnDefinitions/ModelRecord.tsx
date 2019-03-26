import * as React from 'react';
import { Link } from 'react-router-dom';
import routes from 'routes';
import styles from './ColumnDefs.module.css';

class ModelRecordColDef extends React.Component<any> {
  public render() {
    const { id, projectId, experimentId } = this.props.data;
    return (
      <div className={styles.param_cell}>
        <Link
          className={styles.model_link}
          to={routes.modelRecord.getRedirectPath({
            projectId,
            modelRecordId: id
          })}
        >
          <strong>Model ID</strong> {' : '}
          {`${id.slice(0, 4)}...${id.slice(-4)}`}
        </Link>
        <div className={styles.experiment_link}>
          <span className={styles.parma_link_label}> Project ID:</span>
          <span className={styles.parma_link_value}>{projectId.slice(0, 4) + '..'}</span>
        </div>
        <div className={styles.experiment_link}>
          <span className={styles.parma_link_label}> Experiment ID:</span>
          <span className={styles.parma_link_value}>{experimentId.slice(0, 4) + '..'}</span>
        </div>
      </div>
    );
  }
}

export default ModelRecordColDef;
