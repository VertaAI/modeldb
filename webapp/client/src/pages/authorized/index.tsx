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
  selectCurrentWorkspaceName,
} from 'store/workspaces';

import NotFoundPage from './NotFoundPage/NotFoundPage';

import { datasetPages } from './DatasetPages';
import { projectsPages } from './ProjectsPages';
import { versioningPages } from './VersioningPages';

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

// pages should be mapped only one time
const pages = [
  datasetPages,
  projectsPages,
  versioningPages,
].map(({ getPages }) => getPages());

class AuthorizedPages extends React.Component<AllProps> {
  public render() {
    const {
      currentWorkspaceName,
      isCurrentWorkspaceExisted,
      location,
    } = this.props;
    const defaultPagePathname = routes.projects.getRedirectPath({
      workspaceName: currentWorkspaceName,
    });

    const isPageForUnauthorizedUser = Object.values(routes).some(
      route =>
        route.allowedUserType === 'unauthorized' &&
        Boolean(route.getMatch(location.pathname))
    );
    if (isPageForUnauthorizedUser) {
      return <Redirect to={defaultPagePathname} />;
    }

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

        {pages}

        <Route component={NotFoundPage} />
      </Switch>
    );
  }
}

export default withRouter(connect(mapStateToProps)(AuthorizedPages));
