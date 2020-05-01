import * as React from 'react';
import { NavLink } from 'react-router-dom';

import CopyButton from 'core/shared/view/elements/CopyButton/CopyButton';
import { IWorkspace } from 'models/Workspace';
import routes from 'routes';

import styles from './ExperimentRunsInfo.module.css';
import Table, { IRow } from './Table/Table';
import Placeholder from 'core/shared/view/elements/Placeholder/Placeholder';

interface ILocalProps {
  data: IRow[];
  workspaceName: IWorkspace['name'];
}

class ExperimentRunsInfo extends React.PureComponent<ILocalProps> {
  public render() {
    const { data, workspaceName } = this.props;
    return data.length > 0 ? (
      <div className={styles.root}>
        <Table data={data}>
          <Table.Column
            title="Project"
            type="name"
            render={({ project }) => (
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
            )}
          />
          <Table.Column
            title="Experiment"
            type="Experiment"
            render={({ experimentRun: { shortExperiment } }) => (
              <div className={styles.link}>{shortExperiment.name}</div>
            )}
          />
          <Table.Column
            title="Run name"
            type="runName"
            render={({ project, experimentRun }) => (
              <NavLink
                title={experimentRun.name}
                className={styles.link}
                to={routes.modelRecord.getRedirectPath({
                  projectId: project.id,
                  modelRecordId: experimentRun.id,
                  workspaceName,
                })}
              >
                {experimentRun.name}
              </NavLink>
            )}
          />
          <Table.Column
            title="Run Id"
            type="runId"
            render={({ project, experimentRun }) => (
              <div className={styles.experimentRunIdContainer}>
                <NavLink
                  title={name}
                  className={styles.link}
                  to={routes.modelRecord.getRedirectPath({
                    projectId: project.id,
                    modelRecordId: experimentRun.id,
                    workspaceName,
                  })}
                >
                  {experimentRun.id.slice(0, 7)}...
                </NavLink>
                &nbsp;
                <CopyButton value={experimentRun.id} />
              </div>
            )}
          />
        </Table>
      </div>
    ) : (
      <Placeholder>No experiment runs</Placeholder>
    );
  }
}

export default ExperimentRunsInfo;
