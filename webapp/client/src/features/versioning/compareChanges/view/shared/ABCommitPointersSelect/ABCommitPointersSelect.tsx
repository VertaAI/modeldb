import React from 'react';
import { useHistory } from 'react-router';

import { IRepository } from 'shared/models/Versioning/Repository';
import {
  CommitTag,
  CommitPointer,
  RepositoryBranches,
} from 'shared/models/Versioning/RepositoryData';
import BranchesAndTagsList from 'shared/view/domain/Versioning/RepositoryData/BranchesAndTagsList/BranchesAndTagsList';
import { Icon } from 'shared/view/elements/Icon/Icon';
import routes from 'shared/routes';

import styles from './ABCommitPointersSelect.module.css';

interface ILocalProps {
  route:
    | typeof routes.repositoryMergeConflicts
    | typeof routes.repositoryCompareChanges;
  commitPointerA: CommitPointer;
  commitPointerB: CommitPointer;
  tags: CommitTag[];
  branches: RepositoryBranches;
  repository: IRepository;
}

const ABCommitPointersSelect: React.FC<ILocalProps> = ({
  commitPointerA,
  commitPointerB,
  branches,
  tags,
  repository,
}) => {
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
    </div>
  );
};

export default ABCommitPointersSelect;
