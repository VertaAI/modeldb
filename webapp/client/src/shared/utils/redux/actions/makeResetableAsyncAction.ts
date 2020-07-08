import {
  createAsyncAction,
  createAction,
  PayloadAction,
} from 'typesafe-actions';

import { IResetableAsyncAction } from './types';

export default function makeResetableAsyncAction<
  RequestT extends string,
  SuccessT extends string,
  FailureT extends string,
  ResetT extends string
>(
  requestType: RequestT,
  successType: SuccessT,
  failureType: FailureT,
  resetType: ResetT
) {
  return <
    RequestPayload,
    SuccessPayload,
    FailurePayload,
    ResetPayload = void
  >(): IResetableAsyncAction<
    PayloadAction<RequestT, RequestPayload>,
    PayloadAction<SuccessT, SuccessPayload>,
    PayloadAction<FailureT, FailurePayload>,
    PayloadAction<ResetT, ResetPayload>
  > => {
    const actionCreator = createAsyncAction(
      requestType,
      successType,
      failureType
    )<RequestPayload, SuccessPayload, FailurePayload>();
    const reset = createAction(resetType)();
    (actionCreator as any).reset = reset;
    return (actionCreator as (typeof actionCreator & {
      reset: typeof reset;
    })) as any;
  };
}
