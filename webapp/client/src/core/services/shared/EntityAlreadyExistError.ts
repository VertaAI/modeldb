import {
  CustomApiErrorConverter,
  IHttpMethodRequestConfig,
} from 'services/BaseDataService';

export const entityAlreadyExistsError = 'entityAlreadyExists';
export type EntityAlreadyExistsErrorType = typeof entityAlreadyExistsError;

export const errorConverter: Record<
  EntityAlreadyExistsErrorType,
  CustomApiErrorConverter
> = {
  entityAlreadyExists: errorResponse => errorResponse.status === 409,
};

export const addHandlingEntityAlrearyExistsErrorToRequestConfig = (
  requestConfig: IHttpMethodRequestConfig<any>
): IHttpMethodRequestConfig<EntityAlreadyExistsErrorType> => {
  return {
    ...requestConfig,
    errorConverters: { ...requestConfig.errorConverters, ...errorConverter },
  };
};
