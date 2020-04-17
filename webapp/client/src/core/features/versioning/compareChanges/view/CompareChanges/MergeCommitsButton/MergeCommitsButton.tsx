import * as React from 'react';
import { useSelector, useDispatch } from 'react-redux';

import { IRepository } from 'core/shared/models/Versioning/Repository';
import { selectors as compareCommitsSelectors } from 'core/features/versioning/compareCommits';
import Button from 'core/shared/view/elements/Button/Button';
import {
  CommitPointer,
  isMergeCommitsError,
} from 'core/shared/models/Versioning/RepositoryData';
import {
  toastCommunicationError,
  toastError,
} from 'core/shared/view/elements/Notification/Notification';

import { ICommitPointersCommits } from '../../../store/types';
import { selectors } from '../../../store';
import { mergeCommits } from '../../../store/actions';

const useMergeCommitsButton = () => {
  const diffs = useSelector(compareCommitsSelectors.selectDiff);
  const mergingCommits = useSelector(selectors.selectCommunications)
    .mergingCommits;

  const dispatch = useDispatch();

  const MergeCommitsButton = ({
    children,
    repository,
    base,
    commitPointersCommits,
  }: {
    children: (button: any) => any;
    repository: IRepository;
    base: CommitPointer;
    commitPointersCommits: ICommitPointersCommits;
  }) =>
    diffs && diffs.length > 0
      ? children(
          <Button
            theme="secondary"
            size="small"
            onClick={() => {
              dispatch(
                mergeCommits({
                  base,
                  repositoryId: repository.id,
                  repositoryName: repository.name,
                  commitASha: commitPointersCommits.commitPointerA.sha,
                  commitBSha: commitPointersCommits.commitPointerB.sha,
                })
              );
            }}
          >
            Merge
          </Button>
        )
      : children(null);

  React.useEffect(() => {
    if (mergingCommits.error) {
      if (isMergeCommitsError(mergingCommits.error)) {
        toastError('Merge not possible!');
      } else {
        toastCommunicationError(mergingCommits.error.appError);
      }
    }
  }, [mergingCommits.error]);
  React.useEffect(() => {
    return () => {
      dispatch(mergeCommits.reset());
    };
  }, []);

  return { MergeCommitsButton, mergingCommits };
};

export { useMergeCommitsButton };
