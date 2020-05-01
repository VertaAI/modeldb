import { UnavailableEntityApiErrorType } from 'core/services/shared/UnavailableEntityApiError';
import { AppError } from 'core/shared/models/Error';
import {
  IHydratedCommit,
  CommitPointer,
  IMergeCommitsError,
} from 'core/shared/models/Versioning/RepositoryData';
import { ICommunication } from 'core/shared/utils/redux/communication';

export interface ICompareChangesState {
  data: {
    commitPointersCommits: ICommitPointersCommits | null;
  };
  communications: {
    loadingCommitPointersCommits: ICommunication<
      AppError<UnavailableEntityApiErrorType>
    >;
    mergingCommits: ICommunication<MergeCommitCommunicationError>;
  };
}

export type MergeCommitCommunicationError =
  | IMergeCommitsError
  | { type: 'error'; appError: AppError };

export type ICommitPointersCommits = Record<
  keyof IComparedCommitPointersInfo,
  IHydratedCommit
>;

export interface IComparedCommitPointersInfo {
  commitPointerA: CommitPointer;
  commitPointerB: CommitPointer;
}
