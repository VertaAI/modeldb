import cn from 'classnames';
import * as React from 'react';
import { connect } from 'react-redux';
import { useHistory } from 'react-router';
import { Dispatch, bindActionCreators } from 'redux';
import routes from 'routes';

import DefaultMatchRemoteDataWithReloading from 'core/shared/view/elements/MatchRemoteDataComponents/DefaultMatchRemoteDataWithReloading';
import { CompareCommits } from 'core/features/compareCommits';
import { IRepository } from 'core/shared/models/Repository/Repository';
import {
  CommitPointer,
  CommitTag,
  RepositoryBranches,
  CommitPointerHelpers,
} from 'core/shared/models/Repository/RepositoryData';
import BranchesAndTagsList from 'core/shared/view/domain/Repository/RepositoryData/BranchesAndTagsList/BranchesAndTagsList';
import InlineCommunicationError from 'core/shared/view/elements/Errors/InlineCommunicationError/InlineCommunicationError';
import Preloader from 'core/shared/view/elements/Preloader/Preloader';
import { IApplicationState } from 'store/store';

import { actions, selectors } from '../../store';
import styles from './CompareChanges.module.css';

interface ILocalProps {
  repository: IRepository;
  commitPointerValueA: CommitPointer['value'];
  commitPointerValueB: CommitPointer['value'];

  tags: CommitTag[];
  branches: RepositoryBranches;
}

const mapStateToProps = (state: IApplicationState) => ({
  loadingCommitPointersCommits: selectors.selectCommunications(state)
    .loadingCommitPointersCommits,
  commitPointersCommits: selectors.selectCommitPointersCommits(state),
});

const mapDispatchToProps = (dispatch: Dispatch) => {
  return bindActionCreators(
    {
      loadCommitPointersCommits: actions.loadCommitPointersCommits,
    },
    dispatch
  );
};

type AllProps = ILocalProps &
  ReturnType<typeof mapStateToProps> &
  ReturnType<typeof mapDispatchToProps>;

const CompareChanges = (props: AllProps) => {
  const {
    branches,
    tags,
    commitPointerValueA,
    commitPointerValueB,
    commitPointersCommits,
    loadCommitPointersCommits,
    loadingCommitPointersCommits,
    repository,
  } = props;

  const commitPointerA = CommitPointerHelpers.makeCommitPointerFromString(
    commitPointerValueA,
    { branches, tags }
  );
  const commitPointerB = CommitPointerHelpers.makeCommitPointerFromString(
    commitPointerValueB,
    { branches, tags }
  );

  React.useEffect(() => {
    loadCommitPointersCommits({
      repositoryId: repository.id,
      comparedCommitPointersInfo: { commitPointerA, commitPointerB },
    });
  }, [repository.id, commitPointerValueA, commitPointerValueB]);

  const history = useHistory();
  const makeOnChangeCommitPointer = (type: 'A' | 'B') => (
    newCommitPointer: CommitPointer
  ) => {
    const maybeNewCommitPointerA =
      type === 'A' ? newCommitPointer : commitPointerA;
    const maybeNewCommitPointerB =
      type === 'B' ? newCommitPointer : commitPointerB;

    history.push(
      routes.repositoryCompareChanges.getRedirectPathWithCurrentWorkspace({
        repositoryName: repository.name,
        commitPointerAValue: maybeNewCommitPointerA.value,
        commitPointerBValue: maybeNewCommitPointerB.value,
      })
    );
  };

  return (
    <div className={styles.root}>
      <DefaultMatchRemoteDataWithReloading
        communication={loadingCommitPointersCommits}
        data={commitPointersCommits}
      >
        {(loadedCommitPointersCommits, reloadingCommitPointersCommits) => (
          <div
            className={cn({
              [styles.reloading]: reloadingCommitPointersCommits.isRequesting,
            })}
          >
            {reloadingCommitPointersCommits.isRequesting && (
              <div className={styles.reloadingPreloader}>
                <Preloader variant="dots" />
              </div>
            )}
            <div className={styles.comparingInfo}>
              Comparing&nbsp;
              <div className={styles.commitPointer}>
                <BranchesAndTagsList
                  commitPointer={commitPointerA}
                  branches={branches}
                  tags={tags}
                  onChangeCommitPointer={makeOnChangeCommitPointer('A')}
                />
              </div>
              &nbsp;With&nbsp;
              <div className={styles.commitPointer}>
                <BranchesAndTagsList
                  commitPointer={commitPointerB}
                  branches={branches}
                  tags={tags}
                  onChangeCommitPointer={makeOnChangeCommitPointer('B')}
                />
              </div>
            </div>
            {reloadingCommitPointersCommits.error ? (
              <InlineCommunicationError
                error={reloadingCommitPointersCommits.error}
              />
            ) : (
              <div className={styles.compareCommits}>
                <CompareCommits
                  repository={repository}
                  commitASha={loadedCommitPointersCommits.commitPointerA.sha}
                  commitBSha={loadedCommitPointersCommits.commitPointerB.sha}
                />
              </div>
            )}
          </div>
        )}
      </DefaultMatchRemoteDataWithReloading>
    </div>
  );
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(CompareChanges);
