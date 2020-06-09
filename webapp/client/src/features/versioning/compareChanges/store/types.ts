import {
  CommitPointer,
  SHA,
} from 'core/shared/models/Versioning/RepositoryData';
import { Diff } from 'core/shared/models/Versioning/Blob/Diff';

export interface ICompareChangesData {
  commits: ICommitPointersCommits;
  diffs: Diff[];
  isMergeConflict: boolean;
}

export type ICommitPointersCommits = Record<
  keyof IComparedCommitPointersInfo,
  { sha: SHA }
>;

export interface IComparedCommitPointersInfo {
  commitPointerA: CommitPointer;
  commitPointerB: CommitPointer;
}
