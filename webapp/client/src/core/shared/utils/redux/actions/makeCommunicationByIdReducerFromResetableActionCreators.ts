import { AnyAction } from 'redux';
import { getType } from 'typesafe-actions';

import {
  ICommunication,
  makeCommunicationReducerByIdFromEnum,
  initialCommunication,
} from '../communication';
import composeReducers from '../composeReducers';
import { IResetableAsyncAction, GetActionCreatorPayload } from './types';

const makeCommunicationByIdReducerFromResetableActionCreators = <
  T extends IResetableAsyncAction<any, any, any, any>,
  Id extends string | number | symbol
>(
  communicationActionCreators: T,
  idGetters: {
    request: (payload: GetActionCreatorPayload<T['request']>) => Id;
    success: (payload: GetActionCreatorPayload<T['success']>) => Id;
    failure: (payload: GetActionCreatorPayload<T['failure']>) => Id;
    reset: (payload: GetActionCreatorPayload<T['reset']>) => Id;
  }
) => {
  const resetReducer = (
    state: Record<Id, ICommunication<any>> = {} as any,
    action: AnyAction
  ) => {
    if (getType(communicationActionCreators.reset) === action.type) {
      return {
        ...state,
        [idGetters.reset(action.payload)]: initialCommunication,
      };
    }
    return state;
  };

  const reducer = composeReducers([
    makeCommunicationReducerByIdFromEnum(
      {
        REQUEST: getType(communicationActionCreators.request),
        SUCCESS: getType(communicationActionCreators.success),
        FAILURE: getType(communicationActionCreators.failure),
      },
      idGetters
    ),
    resetReducer,
  ]);
  return reducer;
};

export default makeCommunicationByIdReducerFromResetableActionCreators;
