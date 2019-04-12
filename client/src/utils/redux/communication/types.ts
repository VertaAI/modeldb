import { AnyAction } from 'redux';
import { RecordValues } from 'utils/types';

export interface ICommunication<E = string> {
  isRequesting: boolean;
  isSuccess: boolean;
  error: E;
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
    : IAction<T['FAILURE'], string>;
}>;
