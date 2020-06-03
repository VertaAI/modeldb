import { Paper } from '@material-ui/core';
import { bind } from 'decko';
import * as React from 'react';
import { NavLink } from 'react-router-dom';

import Table from 'core/shared/view/elements/Table/Table';
import CopyButton from 'core/shared/view/elements/CopyButton/CopyButton';
import IdView from 'core/shared/view/elements/IdView/IdView';
import ModelRecord from 'models/ModelRecord';
import { IWorkspace } from 'models/Workspace';
import routes from 'routes';

import styles from './DatasetVersionExperimentRuns.module.css';

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
        <Paper>
          <Table
            dataRows={data}
            getRowKey={this.getRowKey}
            columnDefinitions={[
              {
                width: '50%',
                render: ({ id, projectId, name }) => (
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
                ),
                title: 'Name',
                type: 'name',
              },
              {
                width: '50%',
                render: ({ id }) => (
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
                ),
                title: 'Id',
                type: 'id',
              },
            ]}
          />
        </Paper>
      </div>
    );
  }

  @bind
  private getRowKey({ id }: { id: string }) {
    return id;
  }
}

export default DatasetVersionExperimentRuns;
