import {
  CustomApiErrorConverter,
  IHttpMethodRequestConfig,
} from 'services/BaseDataService';
import { HttpError } from 'shared/models/Error';

export type UnavailableEntityApiErrorType =
  | 'entityNotFoundError'
  | 'accessDeniedToEntityError';
export const UnavailableEntityApiErrorType: Record<
  UnavailableEntityApiErrorType,
  UnavailableEntityApiErrorType
> = {
  accessDeniedToEntityError: 'accessDeniedToEntityError',
  entityNotFoundError: 'entityNotFoundError',
};
export type UnavailableEntityApiError = HttpError<
  UnavailableEntityApiErrorType
>;
export type EntityNotFoundError = HttpError<UnavailableEntityApiErrorType>;
export type AccessDeniedToEntityError = HttpError<
  UnavailableEntityApiErrorType
>;

export const errorConverter: Record<
  UnavailableEntityApiErrorType,
  CustomApiErrorConverter
> = {
  accessDeniedToEntityError: ({ status }) => status === 403,
  entityNotFoundError: ({ status }) => {
    return status === 404;
  },
};

export const addHandlingUnavailableEntityErrorToRequestConfig = (
  requestConfig: IHttpMethodRequestConfig<any>
): IHttpMethodRequestConfig<UnavailableEntityApiErrorType> => {
  return {
    ...requestConfig,
    errorConverters: { ...requestConfig.errorConverters, ...errorConverter },
  };
};

export const isUnavailableEntityApiError = (
  error: any
): error is UnavailableEntityApiError => {
  const map: Record<
    UnavailableEntityApiErrorType,
    UnavailableEntityApiErrorType
  > = {
    accessDeniedToEntityError: 'accessDeniedToEntityError',
    entityNotFoundError: 'entityNotFoundError',
  };
  return Boolean(error && Object.values(map).includes(error.type));
};

export const isEntityNotFoundError = (
  error: any
): error is EntityNotFoundError => {
  return (
    error && error.type === UnavailableEntityApiErrorType.entityNotFoundError
  );
};
