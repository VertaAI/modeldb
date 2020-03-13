import * as React from 'react';
import { RouteComponentProps } from 'react-router';

import CompareDatasetVersions from 'components/CompareEntities/CompareDatasetVersions/CompareDatasetVersions';
import routes, { GetRouteParams } from 'routes';

import DatasetDetailsLayout from '../shared/DatasetDetailsLayout/DatasetDetailsLayout';

type RouteProps = RouteComponentProps<
  GetRouteParams<typeof routes.compareDatasetVersions>
>;

type AllProps = RouteProps;

class CompareDatasetVersionsPage extends React.PureComponent<AllProps> {
  public render() {
    const {
      match: {
        params: { datasetId, datasetVersionId1, datasetVersionId2 },
      },
    } = this.props;
    if (!datasetVersionId1 || !datasetVersionId2) {
      console.error('datasetVersionId1 or datasetVersionId2 are undefined!');
      return null;
    }

    return (
      <DatasetDetailsLayout>
        <CompareDatasetVersions
          datasetId={datasetId}
          comparedDatasetVersionIds={[datasetVersionId1, datasetVersionId2]}
        />
      </DatasetDetailsLayout>
    );
  }
}

export default CompareDatasetVersionsPage;
