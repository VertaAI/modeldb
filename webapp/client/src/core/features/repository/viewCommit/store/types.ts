import { UnavailableEntityApiErrorType } from 'core/services/shared/UnavailableEntityApiError';
import { AppError } from 'core/shared/models/Error';
import { IHydratedCommit } from 'core/shared/models/Versioning/RepositoryData';
import { ICommunication } from 'core/shared/utils/redux/communication';

export interface IViewCommitState {
  data: {
    commit: IHydratedCommit | null;
  };
  communications: {
    loadingCommit: ICommunication<AppError<UnavailableEntityApiErrorType>>;
  };
}
