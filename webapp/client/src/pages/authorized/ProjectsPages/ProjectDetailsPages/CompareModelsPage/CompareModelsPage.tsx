import * as React from 'react';
import { RouteComponentProps } from 'react-router';

import CompareModels from 'features/compareModels/view/CompareEntities/CompareModels/CompareModels';
import routes, { GetRouteParams } from 'shared/routes';

import ProjectsPagesLayout from '../../shared/ProjectsPagesLayout/ProjectsPagesLayout';

type RouteProps = RouteComponentProps<
  GetRouteParams<typeof routes.compareModels>
>;

type AllProps = RouteProps;

class CompareModelsPage extends React.PureComponent<AllProps> {
  public render() {
    const {
      match: {
        params: { projectId, modelIds },
      },
    } = this.props;
    return (
      <ProjectsPagesLayout>
        <CompareModels
          projectId={projectId}
          comparedModelIds={modelIds?.split('/') ?? []}
        />
      </ProjectsPagesLayout>
    );
  }
}

export default CompareModelsPage;
