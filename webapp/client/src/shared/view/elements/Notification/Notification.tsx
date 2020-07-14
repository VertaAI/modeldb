import React, { useEffect } from 'react';
import { toast } from 'react-toastify';

import { AppError, isHttpError } from 'shared/models/Error';
import { ICommunication } from 'shared/utils/redux/communication';

import { communicationErrorToString } from '../Errors/InlineCommunicationError/InlineCommunicationError';

export const toastSuccess = (
  content: React.ReactNode,
  options?: { dataTest?: string }
) => {
  toast(<span data-test={options && options.dataTest}>{content}</span>, {
    type: 'success',
  });
};

export const toastError = (
  content: React.ReactNode,
  options?: { toastId?: string }
) => {
  toast(<span>{content}</span>, {
    type: 'error',
    autoClose: false,
    ...(options || {}),
  });
};

export function toastCommunicationError<
  T extends AppError<any>,
  B extends string
>(
  communicationError: T,
  options?: {
    toastId?: string;
    customErrorMessageByType?: T extends AppError<infer ErrorType>
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
  return toastError(errorMessage, options);
}

export const useToastCommunicationError = (communication: ICommunication) => {
  useEffect(() => {
    if (communication.error) {
      toastCommunicationError(communication.error);
    }
  }, [communication.error]);
};
