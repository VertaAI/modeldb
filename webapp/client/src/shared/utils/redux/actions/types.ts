import { ActionCreatorBuilder, PayloadAction } from 'typesafe-actions';

export type GetActionCreatorPayload<
  T extends (...args: any) => PayloadAction<any, any>
> = ReturnType<T>['payload'];

export interface IResetableAsyncAction<
  R extends PayloadAction<any, any>,
  S extends PayloadAction<any, any>,
  F extends PayloadAction<any, any>,
  Reset extends PayloadAction<any, any>
> {
  request: ActionCreatorBuilder<R['type'], R['payload']>;
  success: ActionCreatorBuilder<S['type'], S['payload']>;
  failure: ActionCreatorBuilder<F['type'], F['payload']>;
  reset: ActionCreatorBuilder<Reset['type'], Reset['payload']>;
}
