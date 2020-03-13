import * as BaseActionHelpers from 'core/shared/utils/redux/actions';
import { IApplicationState, IThunkActionDependencies } from 'store/store';

export const makeSimpleApiRequestForApp = <
  ResetableAsyncAction extends BaseActionHelpers.IResetableAsyncAction<
    any,
    any,
    any,
    any
  >
>(
  resetableAsyncAction: ResetableAsyncAction
) => {
  return BaseActionHelpers.makeSimpleApiRequest<
    IApplicationState,
    IThunkActionDependencies,
    ResetableAsyncAction
  >(resetableAsyncAction);
};

export function makeThunkApiRequest<
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
  return BaseActionHelpers.makeThunkApiRequest<
    IApplicationState,
    IThunkActionDependencies,
    RequestType,
    SuccessType,
    FailureType,
    ResetType
  >(requestType, successType, failureType, resetType);
}
