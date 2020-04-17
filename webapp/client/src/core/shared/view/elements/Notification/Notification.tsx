import * as React from 'react';
import { toast } from 'react-toastify';

import { AppError, isHttpError } from 'core/shared/models/Error';

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

export function toastCommunicationError<
  T extends AppError<any>,
  B extends string
>(
  communicationError: T,
  options?: {
    customErrorMessageByType: T extends AppError<infer ErrorType>
      ? ErrorType extends B
        ? Record<B, string>
        : never
      : never;
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