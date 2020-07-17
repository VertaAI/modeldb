import * as React from 'react';
import { connect } from 'react-redux';

import {
  Layout,
  BreadcrumbsBuilder,
  IBreadcrumbsBuilder as IBreadcrumbsBuilder_,
  IMainNavigationRoute,
} from 'features/layout';
import routes from 'shared/routes';
import { IApplicationState } from 'setup/store/store';
import { selectCurrentWorkspaceName } from 'features/workspaces/store';

type ILocalProps = Omit<
  React.ComponentProps<typeof Layout>,
  'userBar' | 'mainNavigationRoutes'
>;

const mapStateToProps = (state: IApplicationState) => {
  return {
    workspaceName: selectCurrentWorkspaceName(state),
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
        iconType: 'datasets',
        text: 'Datasets',
      },
      {
        to: routes.repositories.getRedirectPath({ workspaceName }),
        iconType: 'repository',
        text: 'Repositories',
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
