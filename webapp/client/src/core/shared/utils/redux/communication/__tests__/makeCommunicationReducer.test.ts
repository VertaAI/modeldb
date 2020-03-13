import { AnyAction } from 'redux';
import { action } from 'typesafe-actions';

import { createCodeError } from 'core/shared/models/Error';
import makeCommunicationActionTypes from '../makeCommunicationActionTypes';
import makeCommunicationReducer from '../makeCommunicationReducer';
import { ICommunication } from '../types';

describe('(utils/redux/communication) makeCommunicationReducer', () => {
  const initialState: ICommunication = {
    error: undefined,
    isRequesting: false,
    isSuccess: false,
  };
  const commActionTypes = makeCommunicationActionTypes({
    REQUEST: 'request',
    SUCCESS: 'success',
    FAILURE: 'failure',
  });
  const communicationReducer = makeCommunicationReducer({
    requestType: commActionTypes.REQUEST,
    successType: commActionTypes.SUCCESS,
    failureType: commActionTypes.FAILURE,
  });

  const testCommReducer = (
    name: string,
    action: AnyAction,
    expected: ICommunication
  ) => {
    it(name, () => {
      const res = communicationReducer(initialState, action);
      expect(res).toEqual(expected);
    });
  };

  it('should return correct initial state', () => {
    const res = communicationReducer(undefined, { type: '@@init' });

    expect(res).toEqual(initialState);
  });

  testCommReducer(
    'should return communication in requesting state on request action',
    action(commActionTypes.REQUEST),
    { ...initialState, isRequesting: true }
  );

  testCommReducer(
    'should return communication in successful state on success action',
    action(commActionTypes.SUCCESS),
    { ...initialState, isSuccess: true }
  );

  testCommReducer(
    'should return communication in failure state on failure action',
    action(commActionTypes.FAILURE, createCodeError('error')),
    { ...initialState, error: createCodeError('error') }
  );
});
