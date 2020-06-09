import React, { useEffect } from 'react';
import { useHistory } from 'react-router';

import DiffView from 'core/features/versioning/compareCommits/view/DiffView/DiffView';
import { RepositoryNavigation } from 'core/features/versioning/repositoryNavigation';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import {
  RepositoryBranches,
  CommitTag,
  CommitPointer,
} from 'core/shared/models/Versioning/RepositoryData';
import ComparedCommitsInfo from 'core/shared/view/domain/Versioning/RepositoryData/ComparedCommitsInfo/ComparedCommitsInfo';
import DefaultMatchRemoteData from 'core/shared/view/elements/MatchRemoteDataComponents/DefaultMatchRemoteData';
import { PageCard, PageHeader } from 'core/shared/view/elements/PageComponents';
import Placeholder from 'core/shared/view/elements/Placeholder/Placeholder';
import routes from 'core/shared/routes';

import { useMergeConflictsMutation } from '../../store/mergeConflicts/useMergeConflicts';
import ABCommitPointersSelect from '../shared/ABCommitPointersSelect/ABCommitPointersSelect';
import styles from './MergeConflicts.module.css';
import Button from 'core/shared/view/elements/Button/Button';

interface ILocalProps {
  repository: IRepository;
  commitPointerA: CommitPointer;
  commitPointerB: CommitPointer;
  branches: RepositoryBranches;
  tags: CommitTag[];
}

const MergeConflicts: React.FC<ILocalProps> = ({
  repository,
  commitPointerA,
  commitPointerB,
  branches,
  tags,
}) => {
  const {
    fetchMergeConflicts,
    communicationWithData,
  } = useMergeConflictsMutation();

  useEffect(() => {
    fetchMergeConflicts({
      repositoryId: repository.id,
      commitPointerA,
      commitPointerB,
    });
  }, [repository.id, commitPointerA, commitPointerB]);

  const history = useHistory();

  const redirectToCompareChanges = () => {
    history.push(
      routes.repositoryCompareChanges.getRedirectPathWithCurrentWorkspace({
        commitPointerAValue: commitPointerA.value,
        commitPointerBValue: commitPointerB.value,
        repositoryName: repository.name,
      })
    );
  };

  return (
    <PageCard>
      <PageHeader
        title={repository.name}
        withoutSeparator={true}
        rightContent={<RepositoryNavigation />}
      />
      <DefaultMatchRemoteData
        communication={communicationWithData.communication}
        data={communicationWithData.data}
      >
        {({
          conflicts,
          commonBaseCommit,
          commitA,
          commitB,
          isMergeConflict,
        }) => {
          return isMergeConflict ? (
            <>
              <div className={styles.comparingInfo}>
                <ABCommitPointersSelect
                  repository={repository}
                  commitPointerA={commitPointerA}
                  commitPointerB={commitPointerB}
                  route={routes.repositoryMergeConflicts}
                  branches={branches}
                  tags={tags}
                />
                <div className={styles.button}>
                  <Button
                    theme="secondary"
                    size="small"
                    onClick={redirectToCompareChanges}
                  >
                    See differences
                  </Button>
                </div>
              </div>
              <div className={styles.commitsInfo}>
                <ComparedCommitsInfo
                  commitASha={commitA.sha}
                  commitBSha={commitB.sha}
                  baseCommitSha={
                    commonBaseCommit ? commonBaseCommit.sha : undefined
                  }
                />
              </div>
              {conflicts.map(diff => (
                <DiffView
                  diff={diff}
                  comparedCommitsInfo={{
                    commitA,
                    commitB,
                    commonBaseCommit,
                  }}
                />
              ))}
            </>
          ) : (
            <Placeholder>Nothing to compare</Placeholder>
          );
        }}
      </DefaultMatchRemoteData>
    </PageCard>
  );
};

export default MergeConflicts;
