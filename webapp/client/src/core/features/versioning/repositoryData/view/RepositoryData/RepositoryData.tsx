import cn from 'classnames';
import * as R from 'ramda';
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
import { initialCommunication } from 'core/shared/utils/redux/communication';
import InlineCommunicationError from 'core/shared/view/elements/Errors/InlineCommunicationError/InlineCommunicationError';
import Preloader from 'core/shared/view/elements/Preloader/Preloader';
import { IApplicationState } from 'store/store';

import { actions, selectors } from '../../store';
import DataNavigation from './DataNavigation/DataNavigation';
import styles from './RepositoryData.module.css';
import * as RouteHelpers from './routeHelpers';
import Toolbar from './Toolbar/Toolbar';
import withLoadingRequiredData from './WithLoadingRequiredData/WithLoadingRequiredData';
import InlineErrorView from 'core/shared/view/elements/Errors/InlineErrorView/InlineErrorView';

interface ILocalProps {
  repository: IRepository;
  tags: CommitTag[];
  branches: Branch[];
  onShowNotFoundError(error: any): void;
}

const mapStateToProps = (state: IApplicationState) => ({
  commitWithData: selectors.selectCommitWithData(state),
  loadingCommitWithData: selectors.selectCommunications(state)
    .loadingCommitWithData,
  commitPointer: selectors.selectCommitPointer(state),
});

const mapDispatchToProps = (dispatch: Dispatch) => {
  return bindActionCreators(
    {
      loadCommitWithData: actions.loadCommitWithData,
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
    loadCommitWithData,
    loadingCommitWithData,
    commitWithData,

    changeCommitPointer,
    commitPointer,
    tags,
    branches,
    resetFeatureState,
  } = props;

  const { pathname } = useLocation();
  const fullDataLocationComponents = RouteHelpers.parseFullDataLocationComponentsFromPathname(
    {
      tags,
      branches,
      pathname,
    }
  );
  React.useEffect(() => {
    if (
      fullDataLocationComponents.commitPointer.value !== commitPointer.value
    ) {
      changeCommitPointer(fullDataLocationComponents.commitPointer);
    }
  }, [fullDataLocationComponents.commitPointer]);
  React.useEffect(() => {
    loadCommitWithData({
      repositoryId: repository.id,
      fullDataLocationComponents,
    });
  }, [pathname]);

  React.useEffect(() => {
    return () => {
      resetFeatureState();
    };
  }, []);

  React.useEffect(() => {
    if (isHttpNotFoundError(loadingCommitWithData.error)) {
      onShowNotFoundError(loadingCommitWithData.error);
    }
  }, [loadingCommitWithData.error]);

  return (
    <div className={styles.root} data-test="repository-data">
      {(() => {
        if (
          !commitWithData &&
          (R.equals(loadingCommitWithData, initialCommunication) ||
            loadingCommitWithData.isRequesting)
        ) {
          return (
            <div>
              <Preloader variant="dots" />
            </div>
          );
        }
        if (!commitWithData && loadingCommitWithData.error) {
          return (
            <InlineCommunicationError error={loadingCommitWithData.error} />
          );
        }
        if (!commitWithData) {
          return <InlineErrorView error="Something went wrong!" />;
        }

        return (
          <div
            className={cn({
              [styles.refresh]: loadingCommitWithData.isRequesting,
            })}
          >
            {loadingCommitWithData.isRequesting && (
              <div className={styles.refreshPreloader}>
                <Preloader variant="dots" />
              </div>
            )}
            <div className={styles.toolbar}>
              <Toolbar
                fullDataLocationComponents={fullDataLocationComponents}
                repository={repository}
              />
            </div>
            {!loadingCommitWithData.error ? (
              <div className={styles.navigation}>
                <DataNavigation
                  repository={repository}
                  fullDataLocationComponents={fullDataLocationComponents}
                  commit={commitWithData.commit}
                  data={commitWithData.data}
                />
              </div>
            ) : (
              <InlineCommunicationError error={loadingCommitWithData.error} />
            )}
          </div>
        );
      })()}
    </div>
  );
};

export default withLoadingRequiredData(
  connect(
    mapStateToProps,
    mapDispatchToProps
  )(RepositoryData)
);
