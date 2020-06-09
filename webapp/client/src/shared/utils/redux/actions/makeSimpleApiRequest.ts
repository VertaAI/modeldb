import { Dispatch } from 'redux';
import { ThunkAction } from 'redux-thunk';
import { ActionType } from 'typesafe-actions';

import normalizeError from 'shared/utils/normalizeError';
import { IApplicationState, IThunkActionDependencies } from 'store/store';

import { IResetableAsyncAction, GetActionCreatorPayload } from './types';

const makeSimpleApiRequest = <
  State extends IApplicationState,
  Deps extends IThunkActionDependencies,
  ResetableAsyncAction extends IResetableAsyncAction<any, any, any, any>
>(
  resetableAsyncAction: ResetableAsyncAction
) => (
  requestApi: (params: {
    payload: GetActionCreatorPayload<ResetableAsyncAction['request']>;
    dispatch: Dispatch;
    getState: () => State;
    dependencies: Deps;
  }) => Promise<GetActionCreatorPayload<ResetableAsyncAction['success']>>,
  handlers?: {
    onSuccess: (params: {
      requestPayload: GetActionCreatorPayload<ResetableAsyncAction['request']>;
      successPayload: GetActionCreatorPayload<ResetableAsyncAction['success']>;
      dispatch: Dispatch;
      getState: () => State;
      dependencies: Deps;
    }) => Promise<any>;
  },
  getFailureActionPayload?: ({
    requestPayload,
    error,
  }: {
    requestPayload: GetActionCreatorPayload<ResetableAsyncAction['request']>;
    error: GetActionCreatorPayload<ResetableAsyncAction['failure']>['error'];
    rawError: Error;
  }) => GetActionCreatorPayload<ResetableAsyncAction['failure']>
) => {
  return (
    requestPayload: GetActionCreatorPayload<ResetableAsyncAction['request']>
  ): ThunkAction<void, State, Deps, ActionType<ResetableAsyncAction>> => async (
    dispatch,
    getState,
    dependencies
  ) => {
    dispatch(resetableAsyncAction.request(requestPayload) as any);

    try {
      const res = await requestApi({
        payload: requestPayload,
        dispatch,
        getState,
        dependencies,
      });
      dispatch(resetableAsyncAction.success(res) as any);
      handlers &&
        handlers.onSuccess({
          requestPayload,
          successPayload: res,
          dependencies,
          dispatch,
          getState,
        });
    } catch (e) {
      dispatch(resetableAsyncAction.failure(
        getFailureActionPayload
          ? getFailureActionPayload({
              error: normalizeError(e) as any,
              requestPayload,
              rawError: e,
            })
          : normalizeError(e)
      ) as any);
    }
  };
};

export default makeSimpleApiRequest;
