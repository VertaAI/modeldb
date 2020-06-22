import React from 'react';
import { connect } from 'react-redux';
import {
  Route,
  RouteComponentProps,
  Switch,
  withRouter,
} from 'react-router-dom';
import { bindActionCreators, Dispatch } from 'redux';

import { matchRemoteData } from 'shared/utils/redux/communication/remoteData';
import PageCommunicationError from 'shared/view/elements/Errors/PageCommunicationError/PageCommunicationError';
import Preloader from 'shared/view/elements/Preloader/Preloader';
import AuthorizedLayout from 'pages/authorized/shared/AuthorizedLayout/AuthorizedLayout';
import routes, { GetRouteParams } from 'shared/routes';
import {
  loadDataset,
  selectDataset,
  selectLoadingDataset,
} from 'features/datasets/store';
import { IApplicationState } from 'setup/store/store';
import { selectCurrentWorkspaceName } from 'features/workspaces/store';

import CompareDatasetVersionsPage from './CompareDatasetVersionsPage/CompareDatasetVersionsPage';
import DatasetSummaryPage from './DatasetSummaryPage/DatasetSummaryPage';
import DatasetVersionPage from './DatasetVersionPage/DatasetVersionPage';
import DatasetVersionsPage from './DatasetVersionsPage/DatasetVersionsPage';

const mapDispatchToProps = (dispatch: Dispatch) =>
  bindActionCreators(
    {
      loadDataset,
    },
    dispatch
  );

const mapStateToProps = (state: IApplicationState, routeProps: RouteProps) => {
  return {
    loadingDataset: selectLoadingDataset(
      state,
      routeProps.match.params.datasetId
    ),
    dataset: selectDataset(state, routeProps.match.params.datasetId),
    workspaceName: selectCurrentWorkspaceName(state),
  };
};

type RouteProps = RouteComponentProps<
  GetRouteParams<typeof routes.datasetSummary>
>;

type AllProps = RouteProps &
  ReturnType<typeof mapStateToProps> &
  ReturnType<typeof mapDispatchToProps>;

class DatasetDetailPages extends React.Component<AllProps> {
  public componentDidMount() {
    this.props.loadDataset(
      this.props.match.params.datasetId,
      this.props.match.params.workspaceName
    );
  }

  public componentDidUpdate(prevProps: AllProps) {
    if (prevProps.dataset && !this.props.dataset) {
      this.props.history.replace(
        routes.datasets.getRedirectPath({
          workspaceName: this.props.workspaceName,
        })
      );
    }
  }

  public render() {
    const { loadingDataset, dataset } = this.props;
    return matchRemoteData(loadingDataset, dataset, {
      notAsked: () => (
        <AuthorizedLayout>
          <Preloader variant="dots" />
        </AuthorizedLayout>
      ),
      requesting: () => (
        <AuthorizedLayout>
          <Preloader variant="dots" />
        </AuthorizedLayout>
      ),
      errorOrNillData: ({ error }) => (
        <AuthorizedLayout>
          <PageCommunicationError error={error} />
        </AuthorizedLayout>
      ),
      success: () => (
        <Switch>
          <Route
            exact={true}
            path={routes.datasetSummary.getPath()}
            component={DatasetSummaryPage}
          />
          <Route
            exact={true}
            path={routes.datasetVersions.getPath()}
            component={DatasetVersionsPage}
          />
          <Route
            exact={true}
            path={routes.datasetVersion.getPath()}
            component={DatasetVersionPage}
          />
          <Route
            exact={true}
            path={routes.compareDatasetVersions.getPath()}
            component={CompareDatasetVersionsPage}
          />
        </Switch>
      ),
    });
  }
}

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(withRouter(DatasetDetailPages));
