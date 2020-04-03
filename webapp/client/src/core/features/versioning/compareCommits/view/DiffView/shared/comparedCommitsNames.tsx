import { ICommit } from 'core/shared/models/Versioning/RepositoryData';
import matchType from 'core/shared/utils/matchType';
import { ComparedCommitType } from 'core/shared/models/Versioning/Blob/Diff';
import { shortenSHA } from 'core/shared/view/domain/Versioning/ShortenedSHA/ShortenedSHA';

import { IComparedCommitsInfo } from '../../model';

export function getComparedCommitName<R>(
  f: (fromCommitSha: string, commitSha: ICommit['sha']) => R,
  comparedCommitType: ComparedCommitType,
  commitSha: ICommit['sha']
): R {
  return f(
    `${matchType(
      { A: () => 'From', B: () => 'To' },
      comparedCommitType
    )} Commit SHA:`,
    commitSha
  );
}

export const getColumnComparedCommitsTitles = (
  comparedCommitsInfo: IComparedCommitsInfo
): { A: { title: string }; B: { title: string } } => {
  return {
    A: {
      title: getComparedCommitName(
        (fromCommitSha, commitSha) =>
          `${fromCommitSha} ${shortenSHA(commitSha)}`,
        'A',
        comparedCommitsInfo.commitA.sha
      ),
    },
    B: {
      title: getComparedCommitName(
        (fromCommitSha, commitSha) =>
          `${fromCommitSha} ${shortenSHA(commitSha)}`,
        'B',
        comparedCommitsInfo.commitB.sha
      ),
    },
  };
};
