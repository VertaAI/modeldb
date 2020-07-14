import { Dispatch } from 'redux';

import { AppError } from 'shared/models/Error';
import { IApplicationState, IThunkActionDependencies } from 'setup/store/store';

import makeCommunicationThunk from './makeCommunicationThunk';
import makeSimpleApiRequest from './makeSimpleApiRequest';

export default function makeThunkApiRequest<
  State extends IApplicationState,
  Deps extends IThunkActionDependencies,
  RequestType extends string,
  SuccessType extends string,
  FailureType extends string,
  ResetType extends string
>(
  requestType: RequestType,
  successType: SuccessType,
  failureType: FailureType,
  resetType: ResetType
) {
  return <RequestPayload, SuccessPayload, FailurePayload, ResetPayload = void>(
    requestApi: (params: {
      payload: RequestPayload;
      dispatch: Dispatch;
      getState: () => State;
      dependencies: Deps;
    }) => Promise<SuccessPayload>,
    handlers?: {
      onSuccess: (params: {
        requestPayload: RequestPayload;
        successPayload: SuccessPayload;
        dispatch: Dispatch;
        getState: () => State;
        dependencies: Deps;
      }) => Promise<any>;
    },
    getFailureActionPayload?: ({
      requestPayload,
      error,
    }: {
      requestPayload: RequestPayload;
      error: FailurePayload extends { error: AppError<any> }
        ? FailurePayload['error']
        : never;
      rawError: Error;
    }) => FailurePayload
  ) => {
    return makeCommunicationThunk(
      requestType,
      successType,
      failureType,
      resetType
    )<RequestPayload, SuccessPayload, FailurePayload, ResetPayload>(
      comm =>
        makeSimpleApiRequest(comm)(
          requestApi as any,
          handlers as any,
          getFailureActionPayload as any
        ) as any
    );
  };
}
