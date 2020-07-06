import * as React from 'react';

import { AppError } from 'shared/models/Error';
import { normalizeAppErrorMessage } from 'shared/utils/normalizeErrorData';

import InlineErrorView from '../InlineErrorView/InlineErrorView';

interface ILocalProps {
  error: AppError | Error | string | undefined;
  withoutErrorCode?: boolean;
  customMessage?: string;
  isNillEntity?: boolean;
}

const InlineCommunicationError = (props: ILocalProps) => {
  if (!props.error && !props.isNillEntity) {
    return null;
  }

  const {
    errorCode,
    customMessage,
    message,
  } = getCommunicationErrorTextComponents(props);

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

export const getCommunicationErrorTextComponents = ({
  error,
  isNillEntity,
  customMessage,
  withoutErrorCode = false,
}: {
  error: AppError | Error | string | undefined;
  customMessage?: string;
  isNillEntity?: boolean;
  withoutErrorCode?: boolean;
}) => {
  const message = (() => {
    if (!error && isNillEntity) {
      return 'error';
    }
    return typeof error === 'string'
      ? error
      : normalizeAppErrorMessage(error as any);
  })();

  const errorCode = withoutErrorCode
    ? undefined
    : ((error: any) => {
        if (!error) {
          return '';
        }
        return typeof error.status === 'number' ? error.status : '';
      })(error);

  return {
    errorCode,
    customMessage,
    message,
  };
};

export const communicationErrorTextComponentsToString = ({
  errorCode,
  customMessage,
  message,
}: ReturnType<typeof getCommunicationErrorTextComponents>) => {
  return [errorCode, customMessage, message]
    .filter(Boolean)
    .map(component => component)
    .join(': ');
};

export const communicationErrorToString = (
  error: AppError<any>,
  customMessage?: string
) => {
  return communicationErrorTextComponentsToString(
    getCommunicationErrorTextComponents({ error, customMessage })
  );
};

export const getFormattedCommunicationError = (
  props: Omit<ILocalProps, 'withoutStyles'>
) => {
  const message = (() => {
    if (!props.error && props.isNillEntity) {
      return 'error';
    }
    return typeof props.error === 'string'
      ? props.error
      : normalizeAppErrorMessage(props.error as any);
  })();

  const errorCode = ((error: any) => {
    if (!error) {
      return '';
    }
    return typeof error.status === 'number' ? error.status : '';
  })(props.error);
  return `${errorCode ? `${errorCode}:` : ''} ${message}`;
};

export default InlineCommunicationError;
