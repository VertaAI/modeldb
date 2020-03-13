import { AppError } from 'core/shared/models/Error';
import { ICommunication } from './types';

export const initialCommunication: ICommunication = {
  isRequesting: false,
  isSuccess: false,
  error: undefined,
};
export const requestingCommunication: ICommunication = {
  isRequesting: true,
  isSuccess: false,
  error: undefined,
};
export const successfullCommunication: ICommunication = {
  isRequesting: false,
  error: undefined,
  isSuccess: true,
};
export const makeErrorCommunication = <T extends AppError>(
  error: T
): ICommunication<T> => {
  return { isSuccess: false, isRequesting: false, error };
};
