import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';

import {
  Layout,
  BreadcrumbsBuilder,
  IBreadcrumbsBuilder as IBreadcrumbsBuilder_,
  IMainNavigationRoute,
} from 'core/features/Layout';
import routes from 'routes';
import { IApplicationState } from 'store/store';
import { selectCurrentWorkspaceNameOrDefault } from 'store/workspaces';

type ILocalProps = Omit<
  React.ComponentProps<typeof Layout>,
  'userBar' | 'mainNavigationRoutes'
>;

const mapStateToProps = (state: IApplicationState) => {
  return {
    workspaceName: selectCurrentWorkspaceNameOrDefault(state),
  };
};

type AllProps = ILocalProps & ReturnType<typeof mapStateToProps>;

class AuthorizedLayout extends React.Component<AllProps> {
  public render() {
    const { workspaceName } = this.props;
    const mainNavigationRoutes: IMainNavigationRoute[] = [
      {
        to: routes.projects.getRedirectPath({ workspaceName }),
        iconType: 'folder',
        text: 'Projects',
      },
      {
        to: routes.datasets.getRedirectPath({ workspaceName }),
        iconType: 'bookmarks',
        text: 'Datasets',
      },
    ];

    return (
      <Layout {...this.props} mainNavigationRoutes={mainNavigationRoutes} />
    );
  }
}

export { BreadcrumbsBuilder };
export type IBreadcrumbsBuilder = IBreadcrumbsBuilder_;
export type IAuthorizedLayoutLocalProps = ILocalProps;
export default connect(mapStateToProps)(AuthorizedLayout);
