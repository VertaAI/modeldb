import User from 'models/User';

import { IApplicationState } from '../store';
import { IUserState } from './types';

const selectState = (state: IApplicationState): IUserState => state.layout;

export const selectCurrentUser = (state: IApplicationState): User | null =>
  selectState(state).data.user;

export const selectIsCheckingUserAuthentication = (
  state: IApplicationState
): boolean =>
  selectState(state).communications.checkingUserAuthentication.isRequesting;

export const selectIsUserAuthenticated = (state: IApplicationState): boolean =>
  selectState(state).data.authenticated;
