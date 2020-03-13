import { AnyAction } from 'redux';
import {
  default as makeCommunicationsReducerById,
  IIdGetters,
} from './makeCommunicationsReducerById';
import {
  IAction,
  ICommunicationActionTypes,
  MakeCommunicationActionTypes,
} from './types';

const makeCommunicationReducerByIdFromEnum = <
  T extends MakeCommunicationActionTypes<
    AnyAction,
    AnyAction,
    IAction<any, { error: any }>
  >,
  Id extends string | number | symbol
>(
  { REQUEST, SUCCESS, FAILURE }: ICommunicationActionTypes<any, any, any>,
  idGetters: IIdGetters<T, Id>
) => {
  return makeCommunicationsReducerById({
    idGetters,
    requestType: REQUEST,
    successType: SUCCESS,
    failureType: FAILURE,
  });
};

export default makeCommunicationReducerByIdFromEnum;
