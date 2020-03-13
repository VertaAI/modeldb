import { Action, AnyAction } from 'redux';

import { AppError } from 'core/shared/models/Error';
import { Brand, BrandedRecord } from 'core/shared/utils/Brand';
import { RecordValues } from 'core/shared/utils/types';

export type CommunicationState =
  | 'notAsked'
  | 'requesting'
  | 'success'
  | 'error';
export const CommunicationState: { [K in CommunicationState]: K } = {
  error: 'error',
  notAsked: 'notAsked',
  requesting: 'requesting',
  success: 'success',
};

export interface ICommunication<E = AppError> {
  isRequesting: boolean;
  isSuccess: boolean;
  error: E | undefined;
}

export interface IAction<T, P> {
  type: T;
  payload: P;
}

export interface ICommunicationActionTypes<
  R extends string,
  S extends string,
  F extends string
> {
  REQUEST: R;
  SUCCESS: S;
  FAILURE: F;
}

export interface MakeCommunicationActionTypes<
  R extends AnyAction = AnyAction,
  S extends AnyAction = AnyAction,
  F extends IAction<any, any> = IAction<any, any>
> {
  request: R;
  success: S;
  failure: F;
}

export type MakeCommunicationActions<
  T extends ICommunicationActionTypes<any, any, any>,
  P extends { request?: any; success?: any; failure?: any }
> = RecordValues<{
  request: P extends { request: any }
    ? IAction<T['REQUEST'], P['request']>
    : { type: T['REQUEST'] };
  success: P extends { success: any }
    ? IAction<T['SUCCESS'], P['success']>
    : { type: T['SUCCESS'] };
  failure: P extends { failure: any }
    ? IAction<T['FAILURE'], P['failure']>
    : IAction<T['FAILURE'], AppError>;
}>;

export type ICommunicationById<
  Id extends string | number | symbol = string,
  Error = AppError
> = Record<Id, ICommunication<Error>>; // todo improve ICommunication<Error> | undefined
export type IBrandedCommunicationById<
  B extends Brand<any, any, any>,
  Error = AppError
> = BrandedRecord<B, ICommunication<Error>>;

export interface CommunicationActionsToObj<
  T extends MakeCommunicationActions<any, any>,
  C extends ICommunicationActionTypes<any, any, any>
> {
  request: Extract<T, Action<C['REQUEST']>>;
  success: Extract<T, Action<C['SUCCESS']>>;
  failure: Extract<T, Action<C['FAILURE']>>;
}
