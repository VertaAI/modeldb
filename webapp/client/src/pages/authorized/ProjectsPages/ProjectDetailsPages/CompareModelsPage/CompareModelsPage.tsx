import * as React from 'react';
import { RouteComponentProps } from 'react-router';

import CompareModels from 'components/CompareEntities/CompareModels/CompareModels';
import routes, { GetRouteParams } from 'routes';

import ProjectsPagesLayout from '../../shared/ProjectsPagesLayout/ProjectsPagesLayout';

type RouteProps = RouteComponentProps<
  GetRouteParams<typeof routes.compareModels>
>;

type AllProps = RouteProps;

class CompareModelsPage extends React.PureComponent<AllProps> {
  public render() {
    const {
      match: {
        params: { projectId, modelRecordId1, modelRecordId2 },
      },
    } = this.props;
    if (!modelRecordId1 || !modelRecordId2) {
      console.error('modelRecordId1 or modelRecordId2 are undefined!');
      return null;
    }

    return (
      <ProjectsPagesLayout>
        <CompareModels
          projectId={projectId}
          comparedModelIds={[modelRecordId1, modelRecordId2]}
        />
      </ProjectsPagesLayout>
    );
  }
}

export default CompareModelsPage;
