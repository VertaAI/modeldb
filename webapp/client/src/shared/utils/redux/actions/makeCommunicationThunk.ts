import { ThunkAction } from 'redux-thunk';
import { ActionType, PayloadAction } from 'typesafe-actions';

import makeResetableAsyncAction from './makeResetableAsyncAction';
import { IResetableAsyncAction, GetActionCreatorPayload } from './types';

export type IThunkActionWithResetableAsyncAction<
  State,
  Deps,
  T extends IResetableAsyncAction<any, any, any, any>
> = ((
  payload: GetActionCreatorPayload<T['request']>
) => ThunkAction<void, State, Deps, ActionType<T>>) &
  T;

export default function makeCommunicationThunk<
  State,
  Deps,
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
    ResetPayload = undefined
  >(
    f: (
      communicationActionCreators: IResetableAsyncAction<
        PayloadAction<RequestT, RequestPayload>,
        PayloadAction<SuccessT, SuccessPayload>,
        PayloadAction<FailureT, FailurePayload>,
        PayloadAction<ResetT, undefined>
      >
    ) => (payload: RequestPayload) => ThunkAction<void, State, Deps, any>
  ): IThunkActionWithResetableAsyncAction<
    State,
    Deps,
    IResetableAsyncAction<
      PayloadAction<RequestT, RequestPayload>,
      PayloadAction<SuccessT, SuccessPayload>,
      PayloadAction<FailureT, FailurePayload>,
      PayloadAction<ResetT, ResetPayload>
    >
  > => {
    const resetableAsyncAction = makeResetableAsyncAction(
      requestType,
      successType,
      failureType,
      resetType
    )<RequestPayload, SuccessPayload, FailurePayload>();
    const res: IThunkActionWithResetableAsyncAction<
      State,
      Deps,
      IResetableAsyncAction<
        PayloadAction<RequestT, RequestPayload>,
        PayloadAction<SuccessT, SuccessPayload>,
        PayloadAction<FailureT, FailurePayload>,
        PayloadAction<ResetT, ResetPayload>
      >
    > = f(resetableAsyncAction as any) as any;
    res.request = resetableAsyncAction.request;
    res.success = resetableAsyncAction.success;
    res.failure = resetableAsyncAction.failure;
    res.reset = resetableAsyncAction.reset as any;
    return res as any;
  };
}
