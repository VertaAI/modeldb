import { bind } from 'decko';
import * as React from 'react';
import {
  Route,
  RouteComponentProps,
  RouteProps,
  Switch,
  withRouter,
  Redirect,
} from 'react-router-dom';

import { connect } from 'react-redux';
import routes, { IRoute } from 'core/shared/routes';
import { IApplicationState } from 'store/store';
import {
  selectWorkspaceByName,
  selectCurrentWorkspaceName,
} from 'features/workspaces/store';

import NotFoundPage from './NotFoundPage/NotFoundPage';

import ProjectDetailsPage from './ProjectsPages/ProjectDetailsPages/ProjectDetailsPage';
import ProjectsPage from './ProjectsPages/ProjectsPage/ProjectsPage';

import DatasetDetailPages from './DatasetPages/DatasetDetailPages/DatasetDetailPages';
import DatasetsPage from './DatasetPages/DatasetsPage/DatasetsPage';

import { IRouteWithWorkspace } from 'core/shared/routes/routeWithWorkspace';
import DatasetCreationPage from './DatasetPages/DatasetCreationPage/DatasetCreationPage';
import ProjectCreationPage from './ProjectsPages/ProjectCreationPage/ProjectCreationPage';
import ExperimentCreationPage from './ProjectsPages/ProjectDetailsPages/ExperimentCreationPage/ExperimentCreationPage';
import RepositoriesPage from './VersioningPages/RepositoriesPage/RepositoriesPage';
import RepositoryCreationPage from './VersioningPages/RepositoryCreationPage/RepositoryCreationPage';
import RepositoryDetailsPages from './VersioningPages/RepositoryDetailsPages/RepositoryDetailsPages';
import HighLevelSearchPage from './HighLevelSearchPage/HighLevelSearchPage';

interface IRouteDescription<T extends IRoute<any, any>> {
  route: T | T[];
  Component: Exclude<RouteProps['component'], undefined>;
  isDisabled?: boolean;
}

const mapStateToProps = (state: IApplicationState) => {
  const params = routes.workspace.getMatch(window.location.pathname, false);
  return {
    currentWorkspaceName: selectCurrentWorkspaceName(state),
    isCurrentWorkspaceExisted: params
      ? Boolean(selectWorkspaceByName(state, params.workspaceName))
      : false,
  };
};

type CurrentRouteProps = RouteComponentProps;
type AllProps = CurrentRouteProps & ReturnType<typeof mapStateToProps>;

class Pages extends React.Component<AllProps> {
  public render() {
    const {
      currentWorkspaceName,
      isCurrentWorkspaceExisted,
      location,
    } = this.props;
    const defaultPagePathname = routes.projects.getRedirectPath({
      workspaceName: currentWorkspaceName,
    });

    if (
      isCurrentWorkspaceExisted &&
      routes.workspace.getMatch(location.pathname)
    ) {
      return <Redirect to={defaultPagePathname} />;
    }

    return (
      <Switch>
        <Redirect
          exact={true}
          from={routes.index.getPath()}
          to={defaultPagePathname}
        />

        {this.renderRoutesWithWorkspaces(
          [
            {
              route: routes.highLevelSearch,
              Component: HighLevelSearchPage,
            },
            {
              route: routes.projects,
              Component: ProjectsPage,
            },
            {
              route: routes.projectSummary,
              Component: ProjectDetailsPage,
            },
            {
              route: routes.projectCreation,
              Component: ProjectCreationPage,
            },
            {
              route: routes.experimentCreation,
              Component: ExperimentCreationPage,
            },
            {
              route: routes.modelRecord,
              Component: ProjectDetailsPage,
            },
            {
              route: routes.experimentRuns,
              Component: ProjectDetailsPage,
            },
            { route: routes.charts, Component: ProjectDetailsPage },
            {
              route: routes.experiments,
              Component: ProjectDetailsPage,
            },
            {
              route: routes.compareModels,
              Component: ProjectDetailsPage,
            },

            { route: routes.datasets, Component: DatasetsPage },
            {
              route: routes.datasetCreation,
              Component: DatasetCreationPage,
            },
            {
              route: routes.datasetVersions,
              Component: DatasetDetailPages,
            },
            { route: routes.datasetSummary, Component: DatasetDetailPages },
            { route: routes.datasetVersion, Component: DatasetDetailPages },
            {
              route: routes.compareDatasetVersions,
              Component: DatasetDetailPages,
            },

            {
              route: routes.repositories,
              Component: RepositoriesPage,
            },
            {
              route: routes.createRepository,
              Component: RepositoryCreationPage,
            },
            {
              route: routes.repositoryCommit,
              Component: RepositoryDetailsPages,
            },
            {
              route: routes.repositoryCompareChanges,
              Component: RepositoryDetailsPages,
            },
            {
              route: routes.repositoryCommitsHistory,
              Component: RepositoryDetailsPages,
            },
            {
              route: [routes.repositoryData, routes.repositoryDataWithLocation],
              Component: RepositoryDetailsPages,
            },
            {
              route: routes.repositorySettings,
              Component: RepositoryDetailsPages,
            },
            {
              route: routes.repositoryNetworkGraph,
              Component: RepositoryDetailsPages,
            },
            {
              route: routes.repositoryMergeConflicts,
              Component: RepositoryDetailsPages,
            },
          ],
          isCurrentWorkspaceExisted
        )}

        <Route component={NotFoundPage} />
      </Switch>
    );
  }

  @bind
  private renderRoutesWithoutWorkspaces<T extends IRoute<any, any>>(
    routesDescription: T extends IRouteWithWorkspace<any, any>
      ? never
      : Array<IRouteDescription<IRoute<any, any>>>
  ) {
    return this.renderRoutes(routesDescription);
  }

  @bind
  private renderRoutesWithWorkspaces(
    routesDescription: Array<IRouteDescription<IRouteWithWorkspace<any, any>>>,
    isCurrentWorkspaceExisted: boolean
  ) {
    return this.renderRoutes(routesDescription, component => component);
  }

  @bind
  private renderRoutes<T extends IRoute<any, any>>(
    routesDescription: Array<IRouteDescription<T>>,
    getComponent?: (component: any) => any
  ) {
    return routesDescription
      .filter(route => !route.isDisabled)
      .map(({ route, Component: component }) => (
        <Route
          key={
            Array.isArray(route)
              ? route.map(r => r.getPath()).join('')
              : route.getPath()
          }
          exact={true}
          path={
            Array.isArray(route) ? route.map(r => r.getPath()) : route.getPath()
          }
          component={getComponent ? getComponent(component) : component}
        />
      ));
  }
}

export default withRouter(connect(mapStateToProps)(Pages));
