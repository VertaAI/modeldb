import { HttpError } from 'core/shared/models/Error';
import {
  CustomApiErrorConverter,
  GetErrorMessage,
  IHttpMethodRequestConfig,
} from 'core/services/BaseDataService';

// todo find the better name
export type DeploymentRequestErrorType = 'expectedDeployApiError';
export type DeploymentRequestError = HttpError<DeploymentRequestErrorType>;

export const errorConverter: Record<
  DeploymentRequestErrorType,
  CustomApiErrorConverter
> = {
  expectedDeployApiError: error =>
    Boolean(
      error.error.response &&
        error.error.response.data &&
        !error.error.response.data.code &&
        !/^\d\d\d/.test(error.error.response.data || '')
    ),
};

export const getErrorMessage: GetErrorMessage = error => {
  return error.response && error.response.data
    ? error.response.data
    : undefined;
};

export const config = { responseType: 'text' };

export const addHandlingDeploymentErrorToRequestConfig = (
  requestConfig: IHttpMethodRequestConfig<any>
): IHttpMethodRequestConfig<DeploymentRequestErrorType> => {
  if (requestConfig.getErrorMessage) {
    console.warn('getErrorMessage is overriden');
  }
  return {
    ...requestConfig,
    config: { ...requestConfig, ...config },
    errorConverters: { ...requestConfig.errorConverters, ...errorConverter },
    getErrorMessage,
  };
};

export const isDeploymentError = (
  error: any
): error is DeploymentRequestError => {
  const errorType: DeploymentRequestErrorType = 'expectedDeployApiError';
  return error && error.type === errorType;
};
