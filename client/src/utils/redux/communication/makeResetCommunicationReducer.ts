import { AnyAction } from 'redux';

import initialCommuncation from './initial';
import { ICommunication } from './types';

const makeResetCommunicationReducer = <T>(actionType: T) => (
  state: ICommunication<any> | undefined = initialCommuncation,
  action: AnyAction
) => (action.type === actionType ? initialCommuncation : state);

export default makeResetCommunicationReducer;
