import { AnyAction } from 'redux';

import { initialCommunication } from './communicationStates';
import { ICommunication } from './types';

const makeResetCommunicationReducer = <T>(actionType: T) => (
  state: ICommunication<any> | undefined = initialCommunication,
  action: AnyAction
) => (action.type === actionType ? initialCommunication : state);

export default makeResetCommunicationReducer;
