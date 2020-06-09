import cn from 'classnames';
import React from 'react';
import { useSelector } from 'react-redux';

import CompareCommits from 'core/features/versioning/compareCommits/view/CompareCommits';
import { RepositoryNavigation } from 'core/features/versioning/repositoryNavigation';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import {
  CommitPointer,
  CommitTag,
  RepositoryBranches,
} from 'core/shared/models/Versioning/RepositoryData';
import InlineCommunicationError from 'core/shared/view/elements/Errors/InlineCommunicationError/InlineCommunicationError';
import DefaultMatchRemoteDataWithReloading from 'core/shared/view/elements/MatchRemoteDataComponents/DefaultMatchRemoteDataWithReloading';
import { PageCard, PageHeader } from 'core/shared/view/elements/PageComponents';
import Preloader from 'core/shared/view/elements/Preloader/Preloader';
import { hasAccessToAction } from 'core/shared/models/EntitiesActions';
import routes from 'core/shared/routes';
import { selectCurrentWorkspaceName } from 'features/workspaces/store';

import { useCompareChangesMutation } from '../../store/compareChanges/useCompareChanges';
import ABCommitPointersSelect from '../shared/ABCommitPointersSelect/ABCommitPointersSelect';
import styles from './CompareChanges.module.css';
import MergeCommitsButton from './MergeCommitsButton/MergeCommitsButton';

interface ILocalProps {
  repository: IRepository;
  commitPointerA: CommitPointer;
  commitPointerB: CommitPointer;
  tags: CommitTag[];
  branches: RepositoryBranches;
}

type AllProps = ILocalProps;

const CompareChanges = (props: AllProps) => {
  const { branches, tags, repository, commitPointerA, commitPointerB } = props;

  const workspaceName = useSelector(selectCurrentWorkspaceName);

  const { communication: loadingData, data } = useCompareChangesMutation({
    repositoryId: repository.id,
    commitPointerA,
    commitPointerB,
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
        {(
          { commits, diffs, isMergeConflict },
          reloadingCommitPointersCommits
        ) => {
          const shouldMergeButtonBeShown: boolean =
            hasAccessToAction('update', repository) &&
            diffs &&
            diffs.length > 0;

          return (
            <div
              className={cn({
                [styles.reloading]: reloadingCommitPointersCommits.isRequesting,
              })}
              data-test="compare-changes"
            >
              {reloadingCommitPointersCommits.isRequesting && (
                <div className={styles.reloadingPreloader}>
                  <Preloader variant="dots" />
                </div>
              )}
              <div className={styles.comparingInfo}>
                <ABCommitPointersSelect
                  repository={repository}
                  commitPointerA={commitPointerA}
                  commitPointerB={commitPointerB}
                  route={routes.repositoryCompareChanges}
                  branches={branches}
                  tags={tags}
                />

                {shouldMergeButtonBeShown && (
                  <div className={styles.button}>
                    <MergeCommitsButton
                      isMergeConflict={isMergeConflict}
                      workspaceName={workspaceName}
                      base={commitPointerA}
                      commitPointersCommits={commits}
                      repository={repository}
                      initialCommitPointerA={commitPointerA}
                      initialCommitPointerB={commitPointerB}
                    />
                  </div>
                )}
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

export default CompareChanges;
