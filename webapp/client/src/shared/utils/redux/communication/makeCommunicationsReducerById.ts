import { AnyAction, Reducer } from 'redux';

import {
  IAction,
  ICommunication,
  ICommunicationById,
  MakeCommunicationActionTypes,
} from './types';

export interface IIdGetters<T extends MakeCommunicationActionTypes, Id> {
  request: (payload: T['request']['payload']) => Id;
  success: (payload: T['success']['payload']) => Id;
  failure: (payload: T['failure']['payload']) => Id;
}

const makeCommunicationsReducerById = <
  T extends MakeCommunicationActionTypes<
    AnyAction,
    AnyAction,
    IAction<any, { error: string }>
  >,
  Id extends string | number | symbol
>({
  requestType,
  successType,
  failureType,
  idGetters,
}: {
  requestType: T['request']['type'];
  successType: T['success']['type'];
  failureType: T['failure']['type'];
  idGetters: IIdGetters<T, Id>;
}): Reducer<
  Record<Id, ICommunication<any>>,
  T['request'] | T['failure'] | T['success']
> => {
  return (
    state: ICommunicationById<Id, ICommunication> | undefined = {} as any,
    action: AnyAction
  ): ICommunicationById<Id, ICommunication> => {
    switch (action.type) {
      case requestType: {
        return {
          ...state,
          [idGetters.request(action.payload)]: {
            isSuccess: false,
            isRequesting: true,
            error: undefined,
          },
        };
      }
      case successType: {
        return {
          ...state,
          [idGetters.success(action.payload)]: {
            isSuccess: true,
            isRequesting: false,
            error: undefined,
          },
        };
      }
      case failureType: {
        return {
          ...state,
          [idGetters.failure(action.payload)]: {
            error: action.payload.error || undefined,
            isSuccess: false,
            isRequesting: false,
          },
        };
      }
      default:
        return state;
    }
  };
};

export default makeCommunicationsReducerById;
