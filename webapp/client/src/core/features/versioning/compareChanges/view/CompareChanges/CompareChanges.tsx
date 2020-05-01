import cn from 'classnames';
import * as React from 'react';
import { connect } from 'react-redux';
import { useHistory } from 'react-router';
import { Dispatch, bindActionCreators } from 'redux';
import routes from 'routes';

import { CompareCommits } from 'core/features/versioning/compareCommits';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import {
  CommitPointer,
  CommitTag,
  RepositoryBranches,
  CommitPointerHelpers,
} from 'core/shared/models/Versioning/RepositoryData';
import BranchesAndTagsList from 'core/shared/view/domain/Versioning/RepositoryData/BranchesAndTagsList/BranchesAndTagsList';
import InlineCommunicationError from 'core/shared/view/elements/Errors/InlineCommunicationError/InlineCommunicationError';
import { Icon } from 'core/shared/view/elements/Icon/Icon';
import DefaultMatchRemoteDataWithReloading from 'core/shared/view/elements/MatchRemoteDataComponents/DefaultMatchRemoteDataWithReloading';
import Preloader from 'core/shared/view/elements/Preloader/Preloader';
import { PageCard, PageHeader } from 'core/shared/view/elements/PageComponents';
import { IApplicationState } from 'store/store';
import { RepositoryNavigation } from 'core/features/versioning/repositoryNavigation';

import { actions, selectors } from '../../store';
import styles from './CompareChanges.module.css';
import { useMergeCommitsButton } from './MergeCommitsButton/MergeCommitsButton';

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
    repository,
    commitPointerValueA,
    commitPointerValueB,
    commitPointersCommits,
    loadCommitPointersCommits,
    loadingCommitPointersCommits,
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

  const { MergeCommitsButton, mergingCommits } = useMergeCommitsButton();

  return (
    <PageCard>
      <PageHeader
        title={repository.name}
        withoutSeparator={true}
        rightContent={<RepositoryNavigation />}
      />
      <DefaultMatchRemoteDataWithReloading
        communication={loadingCommitPointersCommits}
        data={commitPointersCommits}
      >
        {(loadedCommitPointersCommits, reloadingCommitPointersCommits) => {
          return (
            <div
              className={cn({
                [styles.reloading]:
                  reloadingCommitPointersCommits.isRequesting ||
                  mergingCommits.isRequesting,
              })}
            >
              {reloadingCommitPointersCommits.isRequesting && (
                <div className={styles.reloadingPreloader}>
                  <Preloader variant="dots" />
                </div>
              )}
              <div className={styles.comparingInfo}>
                <div className={styles.commitPointer}>
                  <BranchesAndTagsList
                    commitPointer={commitPointerA}
                    branches={branches}
                    tags={tags}
                    valueLabel="Base"
                    onChangeCommitPointer={makeOnChangeCommitPointer('A')}
                    dataTest="commit-pointer-a"
                  />
                </div>
                &nbsp;
                <Icon type="arrow-left" />
                &nbsp;
                <div className={styles.commitPointer}>
                  <BranchesAndTagsList
                    commitPointer={commitPointerB}
                    branches={branches}
                    tags={tags}
                    valueLabel="Compare"
                    onChangeCommitPointer={makeOnChangeCommitPointer('B')}
                    dataTest="commit-pointer-b"
                  />
                </div>
                <MergeCommitsButton
                  base={commitPointerA}
                  commitPointersCommits={loadedCommitPointersCommits}
                  repository={repository}
                >
                  {button =>
                    button ? <div className={styles.merge}>{button}</div> : null
                  }
                </MergeCommitsButton>
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
          );
        }}
      </DefaultMatchRemoteDataWithReloading>
    </PageCard>
  );
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(CompareChanges);
