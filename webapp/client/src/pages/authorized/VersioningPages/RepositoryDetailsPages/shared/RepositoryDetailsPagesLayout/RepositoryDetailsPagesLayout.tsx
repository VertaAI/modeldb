import { bind } from 'decko';
import * as React from 'react';

import { IRepository } from 'core/shared/models/Versioning/Repository';
import PagesTabs from 'core/shared/view/pages/PagesTabs/PagesTabs';
import {
  AuthorizedLayout,
  IAuthorizedLayoutLocalProps,
  BreadcrumbsBuilder,
} from 'pages/authorized/shared/AuthorizedLayout';
import routes from 'routes';

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
                label: 'Data',
                to: routes.repositoryData.getRedirectPathWithCurrentWorkspace({
                  repositoryName: repository.name,
                }),
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
        routes: [routes.repositories],
        getName: () => 'Repositories',
      })
      .then({
        routes: [routes.repositoryData, routes.repositorySettings],
        getName: ({ repositoryName }) => repositoryName,
      });
  }
}

export default RepositoryDetailsPagesLayout;
