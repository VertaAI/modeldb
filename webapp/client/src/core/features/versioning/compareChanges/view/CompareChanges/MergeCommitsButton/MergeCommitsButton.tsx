import React, { useCallback, useEffect } from 'react';
import { useHistory } from 'react-router';

import * as CommitComponentLocation from 'core/shared/models/Versioning/CommitComponentLocation';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import { CommitPointer } from 'core/shared/models/Versioning/RepositoryData';
import Button from 'core/shared/view/elements/Button/Button';
import {
  toastError,
  useToastCommunicationError,
} from 'core/shared/view/elements/Notification/Notification';
import { IWorkspace } from 'core/shared/models/Workspace';
import routes from 'core/shared/routes';

import { useMergeCommitsMutation } from '../../../store/mergeCommits/useMergeCommits';
import { ICommitPointersCommits } from '../../../store/types';

const MergeCommitsButton = ({
  initialCommitPointerA,
  initialCommitPointerB,
  repository,
  base,
  commitPointersCommits,
  workspaceName,
  isMergeConflict,
}: {
  initialCommitPointerA: CommitPointer;
  initialCommitPointerB: CommitPointer;
  repository: IRepository;
  base: CommitPointer;
  commitPointersCommits: ICommitPointersCommits;
  workspaceName: IWorkspace['name'];
  isMergeConflict: boolean;
}) => {
  const history = useHistory();

  const { mergeCommits, communicationWithData } = useMergeCommitsMutation();
  useToastCommunicationError(communicationWithData.communication);

  useEffect(() => {
    if (
      communicationWithData.data &&
      communicationWithData.data.isMergeConflict
    ) {
      redirectToMergeConflicts();
      toastError('Merge not possible!');
    }
  }, [communicationWithData.data]);

  const redirectToMergeConflicts = useCallback(
    () =>
      history.push(
        routes.repositoryMergeConflicts.getRedirectPath({
          commitPointerAValue: initialCommitPointerA.value,
          commitPointerBValue: initialCommitPointerB.value,
          repositoryName: repository.name,
          workspaceName,
        })
      ),
    [
      initialCommitPointerA.value,
      initialCommitPointerB.value,
      repository.name,
      workspaceName,
    ]
  );

  const redirectToBase = useCallback(
    () =>
      history.push(
        routes.repositoryDataWithLocation.getRedirectPath({
          workspaceName,
          commitPointer: base,
          location: CommitComponentLocation.makeRoot(),
          repositoryName: repository.name,
          type: 'folder',
        })
      ),
    [base, workspaceName, repository.name]
  );

  const onMergeCommitClick = useCallback(
    () =>
      mergeCommits(
        {
          repositoryId: repository.id,
          base,
          commitASha: commitPointersCommits.commitPointerA.sha,
          commitBSha: commitPointersCommits.commitPointerB.sha,
        },
        redirectToBase
      ),
    [redirectToBase, repository.id, commitPointersCommits]
  );

  return isMergeConflict ? (
    <LocalButton onClick={redirectToMergeConflicts}>
      See merge conflicts
    </LocalButton>
  ) : (
    <LocalButton onClick={onMergeCommitClick} dataTest="merge-commits-button">
      Merge
    </LocalButton>
  );
};

const LocalButton = ({
  children,
  onClick,
  dataTest,
}: {
  children: React.ReactChild;
  onClick: () => void;
  dataTest?: string;
}) => (
  <Button theme="secondary" size="small" dataTest={dataTest} onClick={onClick}>
    {children}
  </Button>
);

export default MergeCommitsButton;
