import User from 'models/User';

import { IApplicationState } from '../store';
import { IUserState } from './types';

const selectState = (state: IApplicationState): IUserState => state.layout;

export const selectCurrentUser = (state: IApplicationState): User | null => selectState(state).user;

export const selectIsCheckingUserAuthentication = (state: IApplicationState): boolean => selectState(state).checkingUserAuthentication;

export const selectIsUserAuthenticated = (state: IApplicationState): boolean => selectState(state).authenticated;
