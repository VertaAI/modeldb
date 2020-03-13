import { Dispatch } from 'redux';

import { AppError } from 'core/shared/models/Error';

import makeCommunicationThunk from './makeCommunicationThunk';
import makeSimpleApiRequest from './makeSimpleApiRequest';

export default function makeThunkApiRequest<
  State,
  Deps,
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
