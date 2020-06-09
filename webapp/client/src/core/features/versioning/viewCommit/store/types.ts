import { UnavailableEntityApiErrorType } from 'services/shared/UnavailableEntityApiError';
import { AppError } from 'core/shared/models/Error';
import {
  IHydratedCommit,
  SHA,
} from 'core/shared/models/Versioning/RepositoryData';
import { ICommunication } from 'core/shared/utils/redux/communication';
import { IExperimentRunInfo } from 'core/shared/models/ModelRecord';
import { Diff } from 'core/shared/models/Versioning/Blob/Diff';

export interface IViewCommitState {
  data: {
    commit: IHydratedCommit | null;
    experimentRunsInfo: IExperimentRunInfo[] | null;
  };
  communications: {
    loadingCommit: ICommunication<AppError<UnavailableEntityApiErrorType>>;
    loadingCommitExperimentRuns: ICommunication<AppError>;
  };
}

export type ICommitView = Pick<
  IHydratedCommit,
  'author' | 'dateCreated' | 'message' | 'sha'
> & { parentSha?: SHA };

export interface ICommitDetails {
  commit: ICommitView;
  experimentRuns: IExperimentRunInfo[];
  diffs: Diff[];
}
