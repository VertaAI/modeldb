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
import routes, { IRoute } from 'routes';
import { IApplicationState } from 'store/store';
import {
  selectWorkspaceByName,
  selectCurrentWorkspaceNameOrDefault,
} from 'store/workspaces';

import NotFoundPage from './NotFoundPage/NotFoundPage';

import ProjectDetailsPage from './ProjectsPages/ProjectDetailsPages/ProjectDetailsPage';
import ProjectsPage from './ProjectsPages/ProjectsPage/ProjectsPage';

import DatasetDetailPages from './DatasetPages/DatasetDetailPages/DatasetDetailPages';
import DatasetsPage from './DatasetPages/DatasetsPage';

import { IRouteWithWorkspace } from 'routes/routeWithWorkspace';
import DatasetCreationPage from './DatasetPages/DatasetCreationPage/DatasetCreationPage';
import ProjectCreationPage from './ProjectsPages/ProjectCreationPage/ProjectCreationPage';
import ExperimentCreationPage from './ProjectsPages/ProjectDetailsPages/ExperimentCreationPage/ExperimentCreationPage';

interface IRouteDescription<T extends IRoute<any, any>> {
  route: T;
  Component: Exclude<RouteProps['component'], undefined>;
  isDisabled?: boolean;
}

const mapStateToProps = (state: IApplicationState) => {
  const params = routes.workspace.getMatch(window.location.pathname, false);
  return {
    currentWorkspaceNameOrDefault: selectCurrentWorkspaceNameOrDefault(state),
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
      currentWorkspaceNameOrDefault,
      isCurrentWorkspaceExisted,
      location,
    } = this.props;
    const defaultPagePathname = routes.projects.getRedirectPath({
      workspaceName: currentWorkspaceNameOrDefault,
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
    return this.renderRoutes(routesDescription, component =>
      isCurrentWorkspaceExisted ? component : NotFoundPage
    );
  }

  @bind
  private renderRoutes<T extends IRoute<any, any>>(
    routesDescription: Array<IRouteDescription<T>>,
    getComponent?: (component: any) => any
  ) {
    return routesDescription.map(({ route, Component: component }) => (
      <Route
        key={route.getPath()}
        exact={true}
        path={route.getPath()}
        component={getComponent ? getComponent(component) : component}
      />
    ));
  }
}

export default withRouter(connect(mapStateToProps)(Pages));
