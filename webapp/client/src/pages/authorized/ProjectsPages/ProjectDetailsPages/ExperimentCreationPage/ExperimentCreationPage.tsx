import React from 'react';
import { RouteComponentProps } from 'react-router-dom';

import { ExperimentCreation } from 'features/experiments';
import routes, { GetRouteParams } from 'shared/routes';

import ProjectsPagesLayout from '../../shared/ProjectsPagesLayout/ProjectsPagesLayout';

type AllProps = RouteComponentProps<
  GetRouteParams<typeof routes.experimentCreation>
>;

class ExperimentCreationPage extends React.PureComponent<AllProps> {
  public render() {
    const { projectId } = this.props.match.params;

    return (
      <ProjectsPagesLayout>
        <ExperimentCreation projectId={projectId} />
      </ProjectsPagesLayout>
    );
  }
}

export default ExperimentCreationPage;
