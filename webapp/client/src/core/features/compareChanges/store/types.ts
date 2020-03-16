import { UnavailableEntityApiErrorType } from 'core/services/shared/UnavailableEntityApiError';
import { AppError } from 'core/shared/models/Error';
import {
  IHydratedCommit,
  CommitPointer,
} from 'core/shared/models/Repository/RepositoryData';
import { ICommunication } from 'core/shared/utils/redux/communication';

export interface ICompareChangesState {
  data: {
    commitPointersCommits: ICommitPointersCommits | null;
  };
  communications: {
    loadingCommitPointersCommits: ICommunication<
      AppError<UnavailableEntityApiErrorType>
    >;
  };
}

export type ICommitPointersCommits = Record<
  keyof IComparedCommitPointersInfo,
  IHydratedCommit
>;

export interface IComparedCommitPointersInfo {
  commitPointerA: CommitPointer;
  commitPointerB: CommitPointer;
}
