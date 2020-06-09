import * as React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps, Switch, Route } from 'react-router';

import { useRepositoryQuery } from 'features/versioning/repositories/store/repositoryQuery/repositoryQuery';
import { matchRemoteData } from 'core/shared/utils/redux/communication/remoteData';
import PageCommunicationError from 'core/shared/view/elements/Errors/PageCommunicationError/PageCommunicationError';
import Preloader from 'core/shared/view/elements/Preloader/Preloader';
import NotFoundPage from 'pages/authorized/NotFoundPage/NotFoundPage';
import { AuthorizedLayout } from 'pages/authorized/shared/AuthorizedLayout';
import routes, { GetRouteParams } from 'core/shared/routes';
import { IApplicationState } from 'store/store';
import { selectCurrentWorkspace } from 'features/workspaces/store';

import CommitPage from './CommitPage/CommitPage';
import CommitsHistoryPage from './CommitsHistoryPage/CommitsHistoryPage';
import CompareChangesPage from './CompareChangesPage/CompareChangesPage';
import MergeConflictsPage from './MergeConflictsPage/MergeConflictsPage';
import NetworkPage from './NetworkPage/NetworkPage';
import RepositoryPage from './RepositoryPage/RepositoryPage';
import RepositorySettingsPage from './RepositorySettingsPage/RepositorySettingsPage';

const mapStateToProps = (state: IApplicationState, props: RouteProps) => {
  return {
    workspace: selectCurrentWorkspace(state),
  };
};

type RouteProps = RouteComponentProps<
  GetRouteParams<typeof routes.repositoryData>
>;

type AllProps = ReturnType<typeof mapStateToProps> & RouteProps;

const RepositoryDetailsPages = ({ workspace, match: { params } }: AllProps) => {
  const {
    data: repository,
    communication: loadingRepository,
  } = useRepositoryQuery({ name: params.repositoryName, workspace });

  const RepositoryNetwork = React.useCallback(
    props => {
      return (
        <NetworkPage
          workspaceName={params.workspaceName}
          repository={repository!}
        />
      );
    },
    [repository, params.workspaceName]
  );

  return matchRemoteData(loadingRepository, repository, {
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
    success: loadedRepository => {
      return (
        <Switch>
          <Route
            exact={true}
            path={routes.repositoryCompareChanges.getPath()}
            render={props => {
              return (
                <CompareChangesPage {...props} repository={loadedRepository} />
              );
            }}
          />
          <Route
            exact={true}
            path={routes.repositoryCommit.getPath()}
            render={props => {
              return <CommitPage {...props} repository={loadedRepository} />;
            }}
          />
          <Route
            exact={true}
            path={routes.repositoryCommitsHistory.getPath()}
            render={props => {
              return (
                <CommitsHistoryPage {...props} repository={loadedRepository} />
              );
            }}
          />
          <Route
            exact={true}
            path={routes.repositoryMergeConflicts.getPath()}
            render={props => {
              return (
                <MergeConflictsPage {...props} repository={loadedRepository} />
              );
            }}
          />
          <Route
            exact={true}
            path={[
              routes.repositoryData.getPath(),
              routes.repositoryDataWithLocation.getPath(),
            ]}
            render={props => {
              return (
                <RepositoryPage {...props} repository={loadedRepository} />
              );
            }}
          />
          <Route
            exact={true}
            path={routes.repositoryNetworkGraph.getPath()}
            component={RepositoryNetwork}
          />
          <Route
            exact={true}
            path={routes.repositorySettings.getPath()}
            render={props => {
              return (
                <RepositorySettingsPage
                  {...props}
                  repository={loadedRepository}
                />
              );
            }}
          />
          <Route component={NotFoundPage} />
        </Switch>
      );
    },
  });
};

export default connect(mapStateToProps)(RepositoryDetailsPages);
