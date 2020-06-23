import {
  ServerError,
  ServerParseError,
  ClientParseError,
} from 'apollo-link-http-common';
import { HttpError } from 'shared/models/Error';

import { defaultErrorMessages } from '../customErrorMessages';

export const isServerError = (error: Error): error is ServerError => {
  return error.name === 'ServerError';
};

export const isServerParseError = (error: Error): error is ServerParseError => {
  return error.name === 'ServerParseError';
};

export const getHttpErrorFromServerError = (
  error: ServerError | ServerParseError
): HttpError<any> => {
  if (isServerError(error)) {
    return new HttpError({
      status: error.statusCode,
      message: error.message.replace(/Network\s+error:\s+/i, '').trim(),
      isMessageUserFriendly: true,
    });
  }
  return new HttpError({
    status: error.statusCode,
    isMessageUserFriendly: true,
    message:
      error.statusCode === 401
        ? defaultErrorMessages.client_error_401
        : error.message,
  });
};

export const isClientParseError = (error: Error): error is ClientParseError => {
  return error.name === 'ServerParseError';
};

export type NetworkErrorWithStatusCode = Error &
  Pick<ServerError, 'statusCode'>;
export const isNetworkErrorWithStatusCode = (
  error: Error
): error is NetworkErrorWithStatusCode => {
  return 'statusCode' in error;
};
