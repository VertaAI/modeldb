import { bind } from 'decko';
import * as React from 'react';
import { NavLink } from 'react-router-dom';

import Table from 'shared/view/elements/Table/Table';
import CopyButton from 'shared/view/elements/CopyButton/CopyButton';
import { PageHeader } from 'shared/view/elements/PageComponents';
import Placeholder from 'shared/view/elements/Placeholder/Placeholder';
import { IExperimentRunInfo } from 'shared/models/ModelRecord';
import { IWorkspace } from 'shared/models/Workspace';
import routes from 'shared/routes';

import styles from './AssociatedExperimentRuns.module.css';

interface ILocalProps {
  data: IExperimentRunInfo[];
  workspaceName: IWorkspace['name'];
}

class AssociatedExperimentRuns extends React.PureComponent<ILocalProps> {
  public render() {
    const { data, workspaceName } = this.props;
    return (
      <div className={styles.root}>
        <PageHeader
          title="Associated experiment runs"
          size="small"
          withoutSeparator={true}
        />
        {data.length > 0 ? (
          <Table
            dataRows={data}
            getRowKey={this.getRowKey}
            columnDefinitions={[
              {
                title: 'Project',
                type: 'name',
                width: '25%',
                render: ({ project }) => (
                  <NavLink
                    title={project.name}
                    className={styles.link}
                    to={routes.projectSummary.getRedirectPath({
                      projectId: project.id,
                      workspaceName,
                    })}
                  >
                    {project.name}
                  </NavLink>
                ),
              },
              {
                title: 'Experiment',
                type: 'Experiment',
                width: '25%',
                render: ({ experiment }) => (
                  <div className={styles.link}>{experiment.name}</div>
                ),
              },
              {
                title: 'Run name',
                type: 'runName',
                width: '25%',
                render: ({ project, name, id }) => (
                  <NavLink
                    title={name}
                    className={styles.link}
                    to={routes.modelRecord.getRedirectPath({
                      projectId: project.id,
                      modelRecordId: id,
                      workspaceName,
                    })}
                  >
                    {name}
                  </NavLink>
                ),
              },
              {
                title: 'Run Id',
                type: 'runId',
                width: '25%',
                render: ({ project, id, name }) => (
                  <div className={styles.experimentRunIdContainer}>
                    <NavLink
                      title={name}
                      className={styles.link}
                      to={routes.modelRecord.getRedirectPath({
                        projectId: project.id,
                        modelRecordId: id,
                        workspaceName,
                      })}
                    >
                      {id.slice(0, 7)}...
                    </NavLink>
                    &nbsp;
                    <CopyButton value={id} />
                  </div>
                ),
              },
            ]}
          />
        ) : (
          <Placeholder withoutCentering={true}>No experiment runs</Placeholder>
        )}
      </div>
    );
  }

  @bind
  private getRowKey(row: IExperimentRunInfo) {
    return row.id;
  }
}

export default AssociatedExperimentRuns;
