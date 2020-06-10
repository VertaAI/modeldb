import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps, withRouter } from 'react-router';

import { Dataset } from 'models/Dataset';
import { IDatasetVersion } from 'models/DatasetVersion';
import routes, { GetRouteParams } from 'routes';
import { selectDatasets } from 'features/datasets/store';
import { selectDatasetVersions } from 'features/datasetVersions/store';
import { IApplicationState } from 'store/store';

import {
  AuthorizedLayout,
  IAuthorizedLayoutLocalProps,
  BreadcrumbsBuilder,
} from 'pages/authorized/shared/AuthorizedLayout';

type ILocalProps = IAuthorizedLayoutLocalProps;

interface IPropsFromState {
  datasets: Dataset[] | null;
  datasetVersions: IDatasetVersion[] | null;
}

type AllProps = IPropsFromState &
  ILocalProps &
  RouteComponentProps<GetRouteParams<typeof routes.datasetSummary>>;

class DatasetsPagesLayout extends React.PureComponent<AllProps> {
  public render() {
    const { filterBarSettings, children } = this.props;
    return (
      <AuthorizedLayout
        breadcrumbsBuilder={this.getBreadcrumbsBuilder()}
        filterBarSettings={filterBarSettings}
      >
        {children}
      </AuthorizedLayout>
    );
  }

  @bind
  private getBreadcrumbsBuilder() {
    const { datasets, datasetVersions } = this.props;
    return BreadcrumbsBuilder()
      .then({
        type: 'single',
        route: routes.datasets,
        getName: () => 'Datasets',
      })
      .then({
        type: 'multiple',
        routes: [
          routes.datasetSummary,
          routes.datasetVersions,
          routes.datasetSettings,
        ],
        checkLoaded: () => Boolean(datasets),
        getName: params => {
          const targetDataset = (datasets || []).find(
            dataset => dataset.id === params.datasetId
          );
          return targetDataset ? targetDataset.name : '';
        },
        redirectTo: routes.datasetSummary,
      })
      .thenOr([
        {
          type: 'single',
          route: routes.compareDatasetVersions,
          getName: () => 'Compare versions',
        },
        {
          type: 'single',
          route: routes.datasetVersion,
          checkLoaded: params =>
            Boolean(
              datasetVersions &&
                datasetVersions.some(
                  datasetVersion =>
                    datasetVersion.id === params.datasetVersionId
                )
            ),
          getName: params => {
            const datasetVersion = (datasetVersions || []).find(
              datasetVersion => datasetVersion.id === params.datasetVersionId
            );
            return datasetVersion
              ? 'Version' + datasetVersion.version
              : 'Version 0';
          },
        },
      ]);
  }
}

const mapStateToProps = (state: IApplicationState): IPropsFromState => {
  return {
    datasets: selectDatasets(state),
    datasetVersions: selectDatasetVersions(state),
  };
};

export default withRouter(connect(mapStateToProps)(DatasetsPagesLayout));
