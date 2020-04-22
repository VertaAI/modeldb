import * as React from 'react';
import { NavLink } from 'react-router-dom';

import CopyButton from 'core/shared/view/elements/CopyButton/CopyButton';
import Placeholder from 'core/shared/view/elements/Placeholder/Placeholder';
import { IWorkspace } from 'models/Workspace';
import routes from 'routes';
import { PageHeader } from 'core/shared/view/elements/PageComponents';

import styles from './AssociatedExperimentRuns.module.css';
import Table, { IRow } from './Table/Table';

interface ILocalProps {
  data: IRow[];
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
              render={({ experiment }) => (
                <div className={styles.link}>{experiment.name}</div>
              )}
            />
            <Table.Column
              title="Run name"
              type="runName"
              render={({ project, name, id }) => (
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
              )}
            />
            <Table.Column
              title="Run Id"
              type="runId"
              render={({ project, id }) => (
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
              )}
            />
          </Table>
        ) : (
          <Placeholder withoutCentering={true}>No experiment runs</Placeholder>
        )}
      </div>
    );
  }
}

export default AssociatedExperimentRuns;
