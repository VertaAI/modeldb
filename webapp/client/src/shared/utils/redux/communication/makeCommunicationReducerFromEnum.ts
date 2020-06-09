import makeCommunicationReducer from './makeCommunicationReducer';
import {
  ICommunicationActionTypes,
  MakeCommunicationActionTypes,
} from './types';

const makeCommunicationReducerFromEnum = <
  T extends MakeCommunicationActionTypes<any, any, any>
>({
  REQUEST: request,
  SUCCESS: success,
  FAILURE: failure,
}: ICommunicationActionTypes<any, any, any>) => {
  return makeCommunicationReducer<T>({
    requestType: request,
    successType: success,
    failureType: failure,
  });
};

export default makeCommunicationReducerFromEnum;
