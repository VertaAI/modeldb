import { UnavailableEntityApiErrorType } from 'services/shared/UnavailableEntityApiError';
import { AppError } from 'shared/models/Error';
import { IHydratedCommit, SHA } from 'shared/models/Versioning/RepositoryData';
import { ICommunication } from 'shared/utils/redux/communication';
import { IExperimentRunInfo } from 'shared/models/ModelRecord';
import { Diff } from 'shared/models/Versioning/Blob/Diff';

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
