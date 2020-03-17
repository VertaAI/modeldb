import { AppError } from 'core/shared/models/Error';
import {
  ICommunication,
  ICommunicationById,
} from 'core/shared/utils/redux/communication';

import { UnavailableEntityApiErrorType } from 'core/services/shared/UnavailableEntityApiError';
import { IPagination } from 'core/shared/models/Pagination';
import { IRepository } from 'core/shared/models/Repository/Repository';
import { EntityAlreadyExistsErrorType } from 'core/services/shared/EntityAlreadyExistError';

export interface IRepositoriesState {
  data: {
    repositories: IRepository[] | null;
    pagination: IPagination;
  };
  communications: {
    creatingRepository: ICommunication<AppError<EntityAlreadyExistsErrorType>>;
    loadingRepositories: ICommunication<AppError>;
    loadingRepositoryByName: ICommunicationById<
      IRepository['name'],
      AppError<UnavailableEntityApiErrorType>
    >;
    deletingRepositoryById: ICommunicationById<string, AppError>;
    addingRepositoryLabel: ICommunicationById<IRepository['id'], AppError>;
    deletingRepositoryLabel: ICommunicationById<IRepository['id'], AppError>;
  };
}
