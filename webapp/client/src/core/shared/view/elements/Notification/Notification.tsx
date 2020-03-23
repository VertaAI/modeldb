import * as React from 'react';
import { toast } from 'react-toastify';

import { AppError, isHttpError } from 'core/shared/models/Error';
import { ICommunication } from 'core/shared/utils/redux/communication';

import { communicationErrorToString } from '../Errors/InlineCommunicationError/InlineCommunicationError';

export const toastSuccess = (
  content: React.ReactNode,
  options?: { dataTest?: string }
) => {
  toast(<span data-test={options && options.dataTest}>{content}</span>, {
    type: 'success',
  });
};

export const toastError = (content: React.ReactNode) => {
  toast(<span>{content}</span>, { type: 'error', autoClose: false });
};

function toastCommunicationError<T extends AppError<B>, B extends string>(
  communicationError: T,
  options?: {
    customErrorMessageByType: Record<B, string>;
  }
): void;
function toastCommunicationError<T extends AppError<B>, B extends undefined>(
  communicationError: T,
  options?: {
    customErrorMessageByType: Record<string, string>;
  }
) {
  const errorMessage =
    options &&
    options.customErrorMessageByType &&
    isHttpError(communicationError)
      ? (options.customErrorMessageByType as any)[communicationError.type] ||
        communicationErrorToString(communicationError)
      : communicationErrorToString(communicationError);
  return toastError(errorMessage);
}
export { toastCommunicationError };
