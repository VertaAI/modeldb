import makeCommunicationReducer from './makeCommunicationReducer';
import {
  ICommunicationActionTypes,
  MakeCommunicationActionTypes,
} from './types';

const makeCommunicationReducerFromEnum = <
  T extends MakeCommunicationActionTypes<any, any, any>
>({
  request,
  success,
  failure,
}: ICommunicationActionTypes<any, any, any>) => {
  return makeCommunicationReducer<T>({
    requestType: request,
    successType: success,
    failureType: failure,
  });
};

export default makeCommunicationReducerFromEnum;
