import * as React from 'react';

import { AppError } from 'core/shared/models/Error';
import { normalizeAppErrorMessage } from 'core/shared/utils/normalizeErrorData';

import InlineErrorView from '../InlineErrorView/InlineErrorView';

interface ILocalProps {
  error: AppError | Error | string | undefined;
  customMessage?: string;
  isNillEntity?: boolean;
}

const InlineCommunicationError = ({
  error,
  customMessage,
  isNillEntity,
}: ILocalProps) => {
  if (!error && !isNillEntity) {
    return null;
  }

  const message = (() => {
    if (!error && isNillEntity) {
      return 'error';
    }
    return typeof error === 'string'
      ? error
      : normalizeAppErrorMessage(error as any);
  })();

  const errorCode = ((error: any) => {
    if (!error) {
      return '';
    }
    return typeof error.status === 'number' ? error.status : '';
  })(error);

  return (
    <InlineErrorView
      error={
        <>
          {errorCode ? <>{errorCode}:&nbsp;</> : ''}
          {customMessage ? <>{customMessage}:&nbsp;</> : ''}
          {message}
        </>
      }
    />
  );
};

export default InlineCommunicationError;
