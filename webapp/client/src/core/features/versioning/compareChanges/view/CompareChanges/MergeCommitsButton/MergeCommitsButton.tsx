import * as React from 'react';

import { IRepository } from 'core/shared/models/Versioning/Repository';
import Button from 'core/shared/view/elements/Button/Button';
import { CommitPointer } from 'core/shared/models/Versioning/RepositoryData';
import {
  toastCommunicationError,
  toastError,
} from 'core/shared/view/elements/Notification/Notification';
import { Diff } from 'core/shared/models/Versioning/Blob/Diff';
import { hasAccessToAction } from 'models/EntitiesActions';
import { useHistory } from 'react-router';
import routes from 'routes';
import { IWorkspace } from 'models/Workspace';
import * as CommitComponentLocation from 'core/shared/models/Versioning/CommitComponentLocation';

import { ICommitPointersCommits } from '../../../store/types';
import { useMergeCommitsMutation } from '../../../store/mergeCommits/useMergeCommits';

const useMergeCommitsButton = ({
  workspaceName,
}: {
  workspaceName: IWorkspace['name'];
}) => {
  const { mergeCommits, communicationWithData } = useMergeCommitsMutation();

  React.useEffect(() => {
    if (communicationWithData.communication.error) {
      toastCommunicationError(communicationWithData.communication.error);
    }
  }, [communicationWithData.communication.error]);
  React.useEffect(() => {
    if (
      communicationWithData.data &&
      communicationWithData.data.isMergeConflict
    ) {
      toastError('Merge not possible!');
    }
  }, [communicationWithData.data]);

  const history = useHistory();
  const MergeCommitsButton = ({
    children,
    repository,
    base,
    commitPointersCommits,
    diffs,
  }: {
    children: (button: any) => any;
    repository: IRepository;
    base: CommitPointer;
    diffs: Diff[];
    commitPointersCommits: ICommitPointersCommits;
  }) =>
    hasAccessToAction('update', repository) && diffs && diffs.length > 0
      ? children(
          <Button
            theme="secondary"
            size="small"
            dataTest="merge-commits-button"
            onClick={() => {
              mergeCommits(
                {
                  repositoryId: repository.id,
                  base,
                  commitASha: commitPointersCommits.commitPointerA.sha,
                  commitBSha: commitPointersCommits.commitPointerB.sha,
                },
                () => {
                  history.push(
                    routes.repositoryDataWithLocation.getRedirectPath({
                      workspaceName,
                      commitPointer: base,
                      location: CommitComponentLocation.makeRoot(),
                      repositoryName: repository.name,
                      type: 'folder',
                    })
                  );
                }
              );
            }}
          >
            Merge
          </Button>
        )
      : children(null);

  return {
    MergeCommitsButton,
    mergingCommits: communicationWithData.communication,
  };
};

export { useMergeCommitsButton };
