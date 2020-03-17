import * as React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps, Switch, Route } from 'react-router';
import { bindActionCreators, Dispatch } from 'redux';

import { actions, selectors } from 'core/features/versioning/repositories';
import { initialCommunication } from 'core/shared/utils/redux/communication';
import { matchRemoteData } from 'core/shared/utils/redux/communication/remoteData';
import PageCommunicationError from 'core/shared/view/elements/Errors/PageCommunicationError/PageCommunicationError';
import Preloader from 'core/shared/view/elements/Preloader/Preloader';
import { AuthorizedLayout } from 'pages/authorized/shared/AuthorizedLayout';
import routes, { GetRouteParams } from 'routes';
import { IApplicationState } from 'store/store';

import CommitPage from './CommitPage/CommitPage';
import CommitsHistoryPage from './CommitsHistoryPage/CommitsHistoryPage';
import CompareChangesPage from './CompareChangesPage/CompareChangesPage';
import RepositoryPage from './RepositoryPage/RepositoryPage';
import RepositorySettingsPage from './RepositorySettingsPage/RepositorySettingsPage';

const mapStateToProps = (state: IApplicationState, props: RouteProps) => {
  const repository = selectors.selectRepositoryByName(
    state,
    props.match.params.repositoryName
  );
  return {
    repository,
    loadingRepository:
      selectors.selectCommunications(state).loadingRepositoryByName[
        props.match.params.repositoryName
      ] || initialCommunication,
  };
};

const mapDispatchToProps = (dispatch: Dispatch) => {
  return bindActionCreators(
    {
      loadRepositoryByName: actions.loadRepositoryByName,
      resetLoadingRepositoryByName: actions.loadRepositoryByName.reset,
    },
    dispatch
  );
};

type RouteProps = RouteComponentProps<
  GetRouteParams<typeof routes.repositoryData>
>;

type AllProps = ReturnType<typeof mapStateToProps> &
  ReturnType<typeof mapDispatchToProps> &
  RouteProps;

const RepositoryDetailsPages = (props: AllProps) => {
  const {
    loadingRepository,
    repository,
    loadRepositoryByName,
    resetLoadingRepositoryByName,
    history,
    match: { params },
  } = props;

  React.useEffect(() => {
    loadRepositoryByName({
      name: params.repositoryName,
      workspaceName: params.workspaceName,
    });
  }, [params.repositoryName, params.workspaceName]);
  React.useEffect(() => {
    return () => {
      if (repository && loadingRepository.error) {
        resetLoadingRepositoryByName(repository);
      }
    };
  }, []);

  React.useEffect(() => {
    // when repository is deleted
    if (loadingRepository.isSuccess && !repository) {
      history.replace(
        routes.repositories.getRedirectPathWithCurrentWorkspace({})
      );
    }
  }, [loadingRepository.isSuccess, repository]);

  const RepositoryPageWithRepository = React.useCallback(
    props => {
      return <RepositoryPage {...props} repository={repository!} />;
    },
    [repository]
  );
  const RepositoryCommitsHistoryPageWithRepository = React.useCallback(
    props => {
      return <CommitsHistoryPage {...props} repository={repository!} />;
    },
    [repository]
  );
  const RepositoryCommit = React.useCallback(
    props => {
      return <CommitPage {...props} repository={repository!} />;
    },
    [repository]
  );
  const RepositoryCompareChanges = React.useCallback(
    props => {
      return <CompareChangesPage {...props} repository={repository!} />;
    },
    [repository]
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
            component={RepositoryCompareChanges}
          />
          <Route
            exact={true}
            path={routes.repositoryCommit.getPath()}
            component={RepositoryCommit}
          />
          <Route
            exact={true}
            path={routes.repositoryCommitsHistory.getPath()}
            component={RepositoryCommitsHistoryPageWithRepository}
          />
          <Route
            exact={true}
            path={[
              routes.repositoryData.getPath(),
              routes.repositoryDataWithLocation.getPath(),
            ]}
            component={RepositoryPageWithRepository}
          />
          <Route
            exact={true}
            path={routes.repositorySettings.getPath()}
            component={(
              props: RouteComponentProps<
                GetRouteParams<typeof routes.repositorySettings>
              >
            ) => (
              <RepositorySettingsPage
                {...props}
                repository={loadedRepository}
              />
            )}
          />
        </Switch>
      );
    },
  });
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(RepositoryDetailsPages);
