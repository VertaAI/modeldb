import * as React from 'react';
import { Link } from 'react-router-dom';

import { DeployButton } from 'components/Deploy';
import routes from 'routes';

import styles from './ColumnDefs.module.css';

class ModelRecordColDef extends React.Component<any> {
  public render() {
    const { id, projectId, name } = this.props.data;
    return (
      <div className={styles.param_cell}>
        <Link
          className={styles.model_link}
          to={routes.modelRecord.getRedirectPath({
            projectId,
            modelRecordId: id,
          })}
        >
          <div className={styles.modelName_block}>
            <div className={styles.model_name}>
              {name} &nbsp;
              <i className="fa fa-external-link" />
            </div>
          </div>
        </Link>

        <div className={styles.deploy_link}>
          <DeployButton modelId={id} />
        </div>
      </div>
    );
  }
}

export default ModelRecordColDef;
