import cn from 'classnames';
import * as React from 'react';
import { connect } from 'react-redux';
import { useLocation } from 'react-router';
import { bindActionCreators, Dispatch } from 'redux';

import { isHttpNotFoundError } from 'core/shared/models/Error';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import {
  CommitTag,
  Branch,
} from 'core/shared/models/Versioning/RepositoryData';
import InlineCommunicationError from 'core/shared/view/elements/Errors/InlineCommunicationError/InlineCommunicationError';
import DefaultMatchRemoteDataWithReloading from 'core/shared/view/elements/MatchRemoteDataComponents/DefaultMatchRemoteDataWithReloading';
import Preloader from 'core/shared/view/elements/Preloader/Preloader';
import { IApplicationState } from 'store/store';

import { actions, selectors } from '../../store';
import DataNavigation from './DataNavigation/DataNavigation';
import styles from './RepositoryData.module.css';
import * as RouteHelpers from './routeHelpers';
import Toolbar from './Toolbar/Toolbar';
import withLoadingRequiredData from './WithLoadingRequiredData/WithLoadingRequiredData';

interface ILocalProps {
  repository: IRepository;
  tags: CommitTag[];
  branches: Branch[];
  onShowNotFoundError(error: any): void;
}

const mapStateToProps = (state: IApplicationState) => ({
  commitWithComponent: selectors.selectCommitWithComponent(state),
  loadingCommitWithComponent: selectors.selectCommunications(state)
    .loadingCommitWithComponent,
  commitPointer: selectors.selectCommitPointer(state),
});

const mapDispatchToProps = (dispatch: Dispatch) => {
  return bindActionCreators(
    {
      loadCommitWithComponent: actions.loadCommitWithComponent,
      changeCommitPointer: actions.changeCommitPointer,
      resetFeatureState: actions.resetFeatureState,
    },
    dispatch
  );
};

type AllProps = ILocalProps &
  ReturnType<typeof mapStateToProps> &
  ReturnType<typeof mapDispatchToProps>;

const RepositoryData = (props: AllProps) => {
  const {
    onShowNotFoundError,
    repository,
    loadCommitWithComponent,
    loadingCommitWithComponent,
    commitWithComponent,

    changeCommitPointer,
    commitPointer,
    tags,
    branches,
    resetFeatureState,
  } = props;

  const { pathname } = useLocation();
  const fullCommitComponentLocationComponents = RouteHelpers.parseFullCommitComponentLocationComponentsFromPathname(
    {
      tags,
      branches,
      pathname,
    }
  );
  React.useEffect(() => {
    if (
      fullCommitComponentLocationComponents.commitPointer.value !==
      commitPointer.value
    ) {
      changeCommitPointer(fullCommitComponentLocationComponents.commitPointer);
    }
  }, [fullCommitComponentLocationComponents.commitPointer]);
  React.useEffect(() => {
    loadCommitWithComponent({
      repositoryId: repository.id,
      fullCommitComponentLocationComponents,
    });
  }, [pathname]);

  React.useEffect(() => {
    return () => {
      resetFeatureState();
    };
  }, []);

  React.useEffect(() => {
    if (isHttpNotFoundError(loadingCommitWithComponent.error)) {
      onShowNotFoundError(loadingCommitWithComponent.error);
    }
  }, [loadingCommitWithComponent.error]);

  return (
    <DefaultMatchRemoteDataWithReloading
      communication={loadingCommitWithComponent}
      data={commitWithComponent}
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
  );
};

export default withLoadingRequiredData(
  connect(
    mapStateToProps,
    mapDispatchToProps
  )(RepositoryData)
);
