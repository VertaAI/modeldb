import { EntityErrorType } from 'core/shared/models/Common';
import { AppError } from 'core/shared/models/Error';
import { ICommunication } from 'core/shared/utils/redux/communication';

export type ILoadEntityCommunication = ICommunication<
  AppError<EntityErrorType>
>;
