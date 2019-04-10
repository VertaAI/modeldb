import { combineReducers } from 'redux';

import { makeCommunicationReducerFromEnum } from 'utils/redux/communication';

import {
  authenticateUserActionTypes,
  checkUserAuthenticationActionTypes,
  IUserState,
  logoutActionTypes,
} from '../types';

export default combineReducers<IUserState['communications']>({
  authenticatingUser: makeCommunicationReducerFromEnum(
    authenticateUserActionTypes
  ),
  checkingUserAuthentication: makeCommunicationReducerFromEnum(
    checkUserAuthenticationActionTypes
  ),
  logouting: makeCommunicationReducerFromEnum(logoutActionTypes),
});
