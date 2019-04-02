import * as React from 'react';
import { Link } from 'react-router-dom';

import routes from 'routes';
import { DeployButton } from 'components/Deploy';

import styles from './ColumnDefs.module.css';

class ModelRecordColDef extends React.Component<any> {
  public render() {
    const { id, projectId, experimentId } = this.props.data;
    return (
      <div className={styles.param_cell}>
        {/* <Link
          className={styles.model_link}
          to={routes.modelRecord.getRedirectPath({
            projectId,
            modelRecordId: id,
          })}
        >
          <strong>Model ID</strong> {' : '}
          {`${id.slice(0, 4)}...${id.slice(-4)}`}
        </Link> */}
        {/* <div className={styles.experiment_link}>
          <span className={styles.parma_link_label}> Project ID:</span>
          <span className={styles.parma_link_value}>
            {projectId.slice(0, 4)}
          </span>
        </div>
        <div className={styles.experiment_link}>
          <span className={styles.parma_link_label}> Experiment ID:</span>
          <span className={styles.parma_link_value}>
            {experimentId.slice(0, 4)}
          </span>
        </div> */}

        <div className={styles.record_summary_meta}>
          <div className={styles.modelId_link}>
            <Link
              className={styles.model_link}
              to={routes.modelRecord.getRedirectPath({
                projectId,
                modelRecordId: id,
              })}
            >
              <span className={styles.parma_link_label}>{'Model:'}</span>{' '}
              <span className={styles.parma_link_value}>
                {id.slice(0, 6)}...
              </span>
            </Link>
          </div>
          <this.parmaLink
            label="Experiment:"
            value={experimentId.slice(0, 6)}
          />
          <this.parmaLink label="Project:" value={projectId.slice(0, 6)} />
        </div>
        <div className={styles.deploy_link}>
          <DeployButton modelId={id} />
        </div>
      </div>
    );
  }

  public parmaLink = (props: { label: string; value: string; link?: any }) => {
    const { label, value } = props;
    return (
      <div className={styles.experiment_link}>
        <span className={styles.parma_link_label}>{label}</span>{' '}
        <span className={styles.parma_link_value}>{value.slice(0, 6)}...</span>
      </div>
    );
  };
}

export default ModelRecordColDef;
