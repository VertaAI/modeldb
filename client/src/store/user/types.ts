import User from 'models/User';

export interface IUserState {
  readonly user: User | null;
  loading: boolean; // todo rename
  authenticated: boolean;
  checkingUserAuthentication: boolean;
}

export enum userAuthenticateActionTypes {
  AUTHENTICATE_USER_REQUEST = '@@user/AUTHENTICATE_USER_REQUEST',
  AUTHENTICATE_USER_SUCCESS = '@@user/AUTHENTICATE_USER_SUCСESS',
  AUTHENTICATE_USER_FAILURE = '@@user/AUTHENTICATE_USER_FAILURE'
}
export type userAuthenticateAction =
  | { type: userAuthenticateActionTypes.AUTHENTICATE_USER_REQUEST }
  | { type: userAuthenticateActionTypes.AUTHENTICATE_USER_SUCCESS; payload: User }
  | { type: userAuthenticateActionTypes.AUTHENTICATE_USER_FAILURE };

export enum checkUserAuthenticationActionTypes {
  CHECKING_USER_AUTH_REQUEST = '@@user/CHECKING_USER_AUTH_REQUEST',
  CHECKING_USER_AUTH_SUCCESS = '@@user/CHECKING_USER_AUTH_SUCСESS',
  CHECKING_USER_AUTH_FAILURE = '@@user/CHECKING_USER_AUTH_FAILURE'
}
export type ICheckUserAuthenticationAction =
  | { type: checkUserAuthenticationActionTypes.CHECKING_USER_AUTH_REQUEST }
  | { type: checkUserAuthenticationActionTypes.CHECKING_USER_AUTH_SUCCESS; payload: User | null }
  | { type: checkUserAuthenticationActionTypes.CHECKING_USER_AUTH_FAILURE };

export enum userLogoutActionTypes {
  LOGOUT_USER = '@@user/LOGOUT_USER'
}
export interface IUserLogoutAction {
  type: userLogoutActionTypes.LOGOUT_USER;
}
