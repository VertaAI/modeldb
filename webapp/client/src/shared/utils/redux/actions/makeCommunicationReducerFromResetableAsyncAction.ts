import { getType } from 'typesafe-actions';

import {
  makeCommunicationReducerFromEnum,
  makeResetCommunicationReducer,
} from '../communication';
import composeReducers from '../composeReducers';
import { IResetableAsyncAction } from './types';

const makeCommunicationReducerFromResetableAsyncAction = <
  T extends IResetableAsyncAction<any, any, any, any>
>(
  resetableAsyncAction: T
) => {
  const reducer = makeCommunicationReducerFromEnum({
    REQUEST: getType(resetableAsyncAction.request),
    SUCCESS: getType(resetableAsyncAction.success),
    FAILURE: getType(resetableAsyncAction.failure),
  });
  return composeReducers([
    reducer,
    makeResetCommunicationReducer(getType(resetableAsyncAction.reset)),
  ]);
};

export default makeCommunicationReducerFromResetableAsyncAction;
