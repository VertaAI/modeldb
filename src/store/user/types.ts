import User from 'models/User';

export interface IUserState {
  readonly user: User | null;
  loading: boolean;
  authenticated: boolean;
}

export enum userAuthenticateActionTypes {
  AUTHENTICATE_USER_REQUEST = '@@user/AUTHENTICATE_USER_REQUEST',
  AUTHENTICATE_USER_SUCCESS = '@@user/AUTHENTICATE_USER_SUCCESS',
  AUTHENTICATE_USER_FAILURE = '@@user/AUTHENTICATE_USER_FAILURE'
}

export type userAuthenticateAction =
  | { type: userAuthenticateActionTypes.AUTHENTICATE_USER_REQUEST }
  | { type: userAuthenticateActionTypes.AUTHENTICATE_USER_SUCCESS; payload: User }
  | { type: userAuthenticateActionTypes.AUTHENTICATE_USER_FAILURE };

export enum userLogoutActionTypes {
  LOGOUT_USER = '@@user/LOGOUT_USER'
}

export interface IUserLogoutAction {
  type: userLogoutActionTypes.LOGOUT_USER;
}
