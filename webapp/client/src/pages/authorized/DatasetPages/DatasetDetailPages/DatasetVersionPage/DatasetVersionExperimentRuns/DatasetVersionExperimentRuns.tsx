import * as React from 'react';
import { NavLink } from 'react-router-dom';

import CopyButton from 'core/shared/view/elements/CopyButton/CopyButton';
import IdView from 'core/shared/view/elements/IdView/IdView';
import ModelRecord from 'models/ModelRecord';
import { IWorkspace } from 'models/Workspace';
import routes from 'routes';

import styles from './DatasetVersionExperimentRuns.module.css';
import Table from './Table/Table';

interface ILocalProps {
  data: IRow[];
  isCompacted: boolean;
  workspaceName: IWorkspace['name'];
}

export type IRow = ModelRecord;

class DatasetVersionExperimentRuns extends React.PureComponent<ILocalProps> {
  public render() {
    const { data, isCompacted, workspaceName } = this.props;
    return (
      <div className={styles.root}>
        <Table data={data}>
          <Table.Column
            render={({ id, projectId, name }) => (
              <NavLink
                title={name}
                className={styles.experiment_run_link}
                to={routes.modelRecord.getRedirectPath({
                  projectId,
                  modelRecordId: id,
                  workspaceName,
                })}
              >
                {name}
              </NavLink>
            )}
            title="Name"
            type="name"
          />
          <Table.Column
            render={({ id }) => (
              <div className={styles.experiment_run_id_wrapper}>
                {isCompacted ? (
                  <>
                    <IdView
                      title={id}
                      value={id}
                      sliceStringUpto={6}
                      additionalClassName={styles.experiment_run_id}
                    />
                    {'...'}{' '}
                  </>
                ) : (
                  <>
                    <IdView title={id} value={id} />{' '}
                  </>
                )}
                <span className={styles.copy_id}>
                  <CopyButton value={id} />
                </span>
              </div>
            )}
            title="Id"
            type="id"
          />
        </Table>
      </div>
    );
  }
}

export default DatasetVersionExperimentRuns;
