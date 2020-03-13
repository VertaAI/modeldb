import axios, { AxiosError, AxiosPromise, AxiosRequestConfig } from 'axios';
import axiosRetry from 'axios-retry';
import * as R from 'ramda';

import { createCodeErrorFromError, HttpError } from 'core/shared/models/Error';
import { commonAPIErrorMessages } from 'core/shared/utils/customErrorMessages';

axiosRetry(axios, {
  retryCondition: error => {
    return (
      error.code === 'ECONNABORTED' ||
      Boolean(error.response && [502, 503, 504].includes(error.response.status))
    );
  },
  shouldResetTimeout: true,
  retries: 3,
  retryDelay: retryNumber => {
    const delays: Record<number, number> = {
      1: 500,
      2: 1000,
      3: 2000,
    };
    return delays[retryNumber] || 3000;
  },
});

export class BaseDataService {
  public constructor() {
    // we have to use absolute baseURL when we use axios-retry. https://github.com/JustinBeckwith/retry-axios/issues/4
    axios.defaults.baseURL = `${window.location.origin}/api`;
    axios.defaults.responseType = 'json';
    axios.defaults.validateStatus = status =>
      (status >= 200 && status < 300) || status === 302;
    axios.defaults.timeout = 30000;
  }

  public get<T = any, ErrorType extends string = string>(
    opt: IHttpMethodRequestConfig<ErrorType>
  ): AxiosPromise<T> {
    return axios.get(opt.url, opt.config).catch(error => {
      if (isAxiosError(error) && opt.errorConverters) {
        throw this.handleCustomApiErrorWithFallback(
          error,
          opt.errorConverters,
          opt.getErrorMessage
        );
      }
      throw this.handleApiError(error, opt.getErrorMessage);
    });
  }

  public post<T = any, ErrorType extends string = string>(
    opt: IHttpMethodRequestConfig<ErrorType>
  ): AxiosPromise<T> {
    return axios.post(opt.url, opt.data, opt.config).catch(error => {
      if (isAxiosError(error) && opt.errorConverters) {
        throw this.handleCustomApiErrorWithFallback(
          error,
          opt.errorConverters,
          opt.getErrorMessage
        );
      }
      throw this.handleApiError(error, opt.getErrorMessage);
    });
  }

  public put<T = any, ErrorType extends string = string>(
    opt: IHttpMethodRequestConfig<ErrorType>
  ): AxiosPromise<T> {
    return axios.put(opt.url, opt.data, opt.config).catch(error => {
      if (isAxiosError(error) && opt.errorConverters) {
        throw this.handleCustomApiErrorWithFallback(
          error,
          opt.errorConverters,
          opt.getErrorMessage
        );
      }
      throw this.handleApiError(error, opt.getErrorMessage);
    });
  }

  public delete<T = any, ErrorType extends string = string>(
    opt: IHttpMethodRequestConfig<ErrorType>
  ): AxiosPromise<T> {
    return axios.delete(opt.url, opt.config).catch(error => {
      if (isAxiosError(error) && opt.errorConverters) {
        throw this.handleCustomApiErrorWithFallback(
          error,
          opt.errorConverters,
          opt.getErrorMessage
        );
      }
      throw this.handleApiError(error, opt.getErrorMessage);
    });
  }

  public handleApiError(
    error: Error | AxiosError,
    getErrorMessage?: GetErrorMessage
  ) {
    if (isAxiosError(error)) {
      const backendMessage = getServerErrorMessage(error);
      throw new HttpError({
        message: getErrorMessage ? getErrorMessage(error) : backendMessage,
        status: error.response!.status,
        type: undefined,
        isMessageUserFriendly: false,
      });
    } else {
      throw createCodeErrorFromError(error);
    }
  }

  public handleCustomApiErrorWithFallback<T extends string>(
    error: Error | AxiosError,
    errorConverters: CustomApiErrorConverters<T>,
    getErrorMessage?: GetErrorMessage
  ) {
    if (isAxiosError(error)) {
      const appropriateCustomErrorPair = R.toPairs(errorConverters).filter(
        ([_, pred]) =>
          (pred as any)({
            error,
            status: error.response!.status,
            serverErrorResponse: error.response!.data,
          })
      )[0];

      if (appropriateCustomErrorPair) {
        throw new HttpError({
          message: getErrorMessage
            ? getErrorMessage(error)
            : (commonAPIErrorMessages as any)[appropriateCustomErrorPair[0]] ||
              appropriateCustomErrorPair[0],
          status: error.response!.status,
          type: appropriateCustomErrorPair[0],
        });
      }
    }
    this.handleApiError(error);
  }
}

export interface IHttpMethodRequestConfig<ErrorType extends string> {
  url: string;
  data?: any;
  config?: AxiosRequestConfig;
  errorConverters?: CustomApiErrorConverters<ErrorType>;
  getErrorMessage?: GetErrorMessage;
}

type CustomApiErrorConverters<T extends string> = Record<
  T,
  CustomApiErrorConverter
>;
export type CustomApiErrorConverter = (props: {
  error: AxiosError;
  status: number;
  serverErrorResponse?: IApiErrorData;
}) => Boolean;
export type GetErrorMessage = (error: AxiosError) => string | undefined;

interface IApiErrorData {
  code: number;
  error: string;
  message: string;
}

function isAxiosError(arg: any): arg is AxiosError {
  return 'response' in arg;
}

function getServerErrorMessage(error: Error | AxiosError): string | undefined {
  if (isAxiosError(error)) {
    return (
      error.response &&
      error.response.data &&
      (error.response.data as IApiErrorData).message
    );
  }
}
