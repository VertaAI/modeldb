import React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps } from 'react-router-dom';

import { handleCustomErrorWithFallback } from 'core/shared/models/Error';
import PageCommunicationError from 'core/shared/view/elements/Errors/PageCommunicationError/PageCommunicationError';
import { selectCommunications, DatasetVersion } from 'features/datasetVersions';
import NotFoundPage from 'pages/authorized/NotFoundPage/NotFoundPage';
import AuthorizedLayout from 'pages/authorized/shared/AuthorizedLayout/AuthorizedLayout';
import routes, { GetRouteParams } from 'routes';
import { IApplicationState } from 'store/store';

import DatasetsPagesLayout from '../../shared/DatasetsPagesLayout/DatasetsPagesLayout';

const mapStateToProps = (state: IApplicationState, routeProps: RouteProps) => {
  return {
    loadingDatasetVersion: selectCommunications(state).loadingDatasetVersion,
  };
};

type RouteProps = RouteComponentProps<
  GetRouteParams<typeof routes.datasetVersion>
>;
type AllProps = ReturnType<typeof mapStateToProps> & RouteProps;

class DatasetVersionPage extends React.PureComponent<AllProps> {
  public render() {
    const {
      loadingDatasetVersion,
      match: {
        params: { datasetId, datasetVersionId },
      },
    } = this.props;

    if (loadingDatasetVersion.error) {
      return handleCustomErrorWithFallback(
        loadingDatasetVersion.error,
        {
          accessDeniedToEntity: () => (
            <NotFoundPage error={loadingDatasetVersion.error} />
          ),
          entityNotFound: () => (
            <NotFoundPage error={loadingDatasetVersion.error} />
          ),
        },

        error => (
          <AuthorizedLayout>
            <PageCommunicationError error={error} />
          </AuthorizedLayout>
        )
      );
    }

    return (
      <DatasetsPagesLayout>
        <DatasetVersion
          datasetId={datasetId}
          datasetVersionId={datasetVersionId}
        />
      </DatasetsPagesLayout>
    );
  }
}

export default connect(mapStateToProps)(DatasetVersionPage);
