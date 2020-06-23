import { EntityErrorType } from 'shared/models/Common';
import { AppError } from 'shared/models/Error';
import { ICommunication } from 'shared/utils/redux/communication';

export type ILoadEntityCommunication = ICommunication<
  AppError<EntityErrorType>
>;
