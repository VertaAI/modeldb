import { HttpError, AppError } from 'core/shared/models/Error';

import {
  defaultErrorMessages,
  commonAPIErrorMessages,
} from './customErrorMessages';

const normalizeApiErrorMessage = (
  apiError: HttpError<string | undefined>
): string => {
  if (apiError && apiError.type && apiError.type in commonAPIErrorMessages) {
    return (commonAPIErrorMessages as any)[apiError.type];
  }
  switch (apiError.category) {
    case 'clientError':
      return defaultErrorMessages.client_error_4xx;
    case 'serverError':
      return defaultErrorMessages.server_error_5xx;
    default:
      return defaultErrorMessages.default_api_error;
  }
};

export const normalizeAppErrorMessage = (
  appError: AppError | Error
): string => {
  switch (appError.name) {
    case 'apiError':
      return normalizeApiErrorMessage(appError as HttpError<
        string | undefined
      >);
    case 'codeError':
      return defaultErrorMessages.default_code_error;
    default:
      return defaultErrorMessages.default_error;
  }
};
