import cn from 'classnames';
import * as React from 'react';
import { useLocation } from 'react-router';

import { isHttpNotFoundError } from 'core/shared/models/Error';
import {
  IRepository,
  IBranchesAndTags,
} from 'core/shared/models/Versioning/Repository';
import InlineCommunicationError from 'core/shared/view/elements/Errors/InlineCommunicationError/InlineCommunicationError';
import DefaultMatchRemoteDataWithReloading from 'core/shared/view/elements/MatchRemoteDataComponents/DefaultMatchRemoteDataWithReloading';
import Preloader from 'core/shared/view/elements/Preloader/Preloader';

import DataNavigation from './DataNavigation/DataNavigation';
import styles from './RepositoryData.module.css';
import * as RouteHelpers from './routeHelpers';
import Toolbar from './Toolbar/Toolbar';
import withLoadingBranchesAndTags from './WithLoadingBranchesAndTags/WithLoadingBranchesAndTags';
import { useRepositoryDataQuery } from '../../store/repositoryData/useRepositoryData';
import Reloading from 'core/shared/view/elements/Reloading/Reloading';

interface ILocalProps extends IBranchesAndTags {
  repository: IRepository;
  onShowNotFoundError(error: any): void;
}

type AllProps = ILocalProps;

const RepositoryData = (props: AllProps) => {
  const {
    onShowNotFoundError,
    repository,

    tags,
    branches,
  } = props;

  const { pathname } = useLocation();
  const fullCommitComponentLocationComponents = RouteHelpers.parseFullCommitComponentLocationComponentsFromPathname(
    {
      tags,
      branches,
      pathname,
    }
  );

  const {
    communication: loadingRepositoryData,
    data: repositoryData,
    refetch,
  } = useRepositoryDataQuery({
    repositoryId: repository.id,
    fullCommitComponentLocation: fullCommitComponentLocationComponents,
  });

  React.useEffect(() => {
    if (isHttpNotFoundError(loadingRepositoryData.error)) {
      onShowNotFoundError(loadingRepositoryData.error);
    }
  }, [loadingRepositoryData.error]);

  return (
    <Reloading onReload={refetch}>
      <DefaultMatchRemoteDataWithReloading
        communication={loadingRepositoryData}
        data={repositoryData}
      >
        {(loadedCommitWithComponent, reloadingCommunication) => (
          <div
            className={cn({
              [styles.refresh]: reloadingCommunication.isRequesting,
            })}
            data-test="repository-data-content"
          >
            {reloadingCommunication.isRequesting && (
              <div
                className={styles.refreshPreloader}
                data-test="refresh-preloader"
              >
                <Preloader variant="dots" />
              </div>
            )}
            <div className={styles.toolbar}>
              <Toolbar
                fullCommitComponentLocationComponents={
                  fullCommitComponentLocationComponents
                }
                repository={repository}
                branches={branches}
                tags={tags}
              />
            </div>
            {!reloadingCommunication.error ? (
              <div className={styles.navigation}>
                <DataNavigation
                  repository={repository}
                  fullCommitComponentLocationComponents={
                    fullCommitComponentLocationComponents
                  }
                  commit={loadedCommitWithComponent.commit}
                  component={loadedCommitWithComponent.component}
                />
              </div>
            ) : (
              <InlineCommunicationError error={reloadingCommunication.error} />
            )}
          </div>
        )}
      </DefaultMatchRemoteDataWithReloading>
    </Reloading>
  );
};

export default withLoadingBranchesAndTags(RepositoryData);
