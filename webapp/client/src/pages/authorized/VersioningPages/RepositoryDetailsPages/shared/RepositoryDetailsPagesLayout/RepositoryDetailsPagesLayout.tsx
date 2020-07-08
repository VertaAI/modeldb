import { bind } from 'decko';
import * as React from 'react';

import { IRepository } from 'shared/models/Versioning/Repository';
import PagesTabs from 'shared/view/pages/PagesTabs/PagesTabs';
import {
  AuthorizedLayout,
  IAuthorizedLayoutLocalProps,
  BreadcrumbsBuilder,
} from 'pages/authorized/shared/AuthorizedLayout';
import routes from 'shared/routes';

import styles from './RepositoryDetailsPagesLayout.module.css';

type ILocalProps = IAuthorizedLayoutLocalProps & {
  isDisabledTabs?: boolean;
  repository: IRepository;
};

type AllProps = ILocalProps;

class RepositoryDetailsPagesLayout extends React.Component<AllProps> {
  public render() {
    const { children, isDisabledTabs, repository } = this.props;
    return (
      <AuthorizedLayout breadcrumbsBuilder={this.getBreadcrumbsBuilder()}>
        <div className={styles.root}>
          <PagesTabs
            tabs={[
              {
                label: 'Contents',
                to: routes.repositoryData.getRedirectPathWithCurrentWorkspace({
                  repositoryName: repository.name,
                }),
              },
              {
                label: 'Network',
                to: routes.repositoryNetworkGraph.getRedirectPathWithCurrentWorkspace(
                  {
                    repositoryName: repository.name,
                  }
                ),
              },
              {
                label: 'Settings',
                to: routes.repositorySettings.getRedirectPathWithCurrentWorkspace(
                  {
                    repositoryName: repository.name,
                  }
                ),
              },
            ]}
            isDisabled={isDisabledTabs}
          />
          <div className={styles.content}>{children}</div>
        </div>
      </AuthorizedLayout>
    );
  }

  @bind
  private getBreadcrumbsBuilder() {
    return BreadcrumbsBuilder()
      .then({
        type: 'single',
        route: routes.repositories,
        getName: () => 'Repositories',
      })
      .then({
        type: 'multiple',
        routes: [
          routes.repositoryData,
          routes.repositorySettings,
          routes.repositoryNetworkGraph,
        ],
        redirectTo: routes.repositoryData,
        getName: ({ repositoryName }) => repositoryName,
      });
  }
}

export default RepositoryDetailsPagesLayout;
