import cn from 'classnames';
import * as React from 'react';

import { AppError } from 'core/shared/models/Error';
import { defaultErrorMessages } from 'core/shared/utils/customErrorMessages';
import { normalizeAppErrorMessage } from 'core/shared/utils/normalizeErrorData';
import DynamicNotFound from 'core/shared/view/elements/NotFoundComponents/DynamicNotFound/DynamicNotFound';

import styles from './PageCommunicationError.module.css';

interface ILocalProps {
  error: AppError | Error | string | undefined;
  isNillEntity?: boolean;
  dataTest?: string;
  dynamicSize?: boolean;
}

const PageCommunicationError = ({
  error,
  dataTest,
  isNillEntity,
  dynamicSize,
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

  const defaultErrorCode = defaultErrorMessages.default_error_code;
  return (
    <div
      className={cn(styles.root, { [styles.dynamic]: dynamicSize })}
      data-test={dataTest || 'page-error'}
    >
      {errorCode ? (
        <DynamicNotFound errorCode={errorCode} errorMessage={message} />
      ) : (
        <DynamicNotFound errorCode={defaultErrorCode} errorMessage={message} />
      )}
    </div>
  );
};

export default PageCommunicationError;
