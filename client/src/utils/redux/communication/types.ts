import { AnyAction } from 'redux';
import { RecordValues } from 'utils/types';

export interface ICommunication<E = string> {
  isRequesting: boolean;
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
  request: R;
  success: S;
  failure: F;
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
  P extends { request?: any; success?: any; failure?: string }
> = RecordValues<{
  request: P extends { request: any }
    ? IAction<T['request'], P['request']>
    : { type: T['request'] };
  success: P extends { success: any }
    ? IAction<T['success'], P['success']>
    : { type: T['success'] };
  failure: P extends { failure: any }
    ? IAction<T['failure'], P['failure']>
    : IAction<T['failure'], string>;
}>;
