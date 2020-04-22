import cn from 'classnames';
import * as React from 'react';
import { useHistory } from 'react-router';
import routes from 'routes';
import { connect } from 'react-redux';

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
import { RepositoryNavigation } from 'core/features/versioning/repositoryNavigation';
import CompareCommits from 'core/features/versioning/compareCommits/view/CompareCommits';
import { IApplicationState } from 'store/store';
import { selectCurrentWorkspaceName } from 'store/workspaces';

import styles from './CompareChanges.module.css';
import { useMergeCommitsButton } from './MergeCommitsButton/MergeCommitsButton';
import { useCompareChangesMutation } from '../../store/compareChanges/useCompareChanges';

interface ILocalProps {
  repository: IRepository;
  commitPointerValueA: CommitPointer['value'];
  commitPointerValueB: CommitPointer['value'];

  tags: CommitTag[];
  branches: RepositoryBranches;
}

const mapStateToProps = (state: IApplicationState) => {
  return {
    workspaceName: selectCurrentWorkspaceName(state),
  };
};

type AllProps = ILocalProps & ReturnType<typeof mapStateToProps>;

const CompareChanges = (props: AllProps) => {
  const {
    branches,
    tags,
    repository,
    commitPointerValueA,
    commitPointerValueB,
    workspaceName,
  } = props;

  const commitPointerA = CommitPointerHelpers.makeCommitPointerFromString(
    commitPointerValueA,
    { branches, tags }
  );
  const commitPointerB = CommitPointerHelpers.makeCommitPointerFromString(
    commitPointerValueB,
    { branches, tags }
  );

  const { communication: loadingData, data } = useCompareChangesMutation({
    repositoryId: repository.id,
    commitPointerA,
    commitPointerB,
  });

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

  const { MergeCommitsButton, mergingCommits } = useMergeCommitsButton({
    workspaceName,
  });

  return (
    <PageCard>
      <PageHeader
        title={repository.name}
        withoutSeparator={true}
        rightContent={<RepositoryNavigation />}
      />
      <DefaultMatchRemoteDataWithReloading
        communication={loadingData}
        data={data}
      >
        {({ commits, diffs }, reloadingCommitPointersCommits) => {
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
                  commitPointersCommits={commits}
                  repository={repository}
                  diffs={diffs}
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
                    commitASha={commits.commitPointerA.sha}
                    commitBSha={commits.commitPointerB.sha}
                    diffs={diffs}
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

export default connect(mapStateToProps)(CompareChanges);
