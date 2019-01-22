import User from 'models/User';

export interface IUserState {
  readonly user: User | null;
}

export enum userAuthenticateActionTypes {
  AUTHENTICATE_USER_REQUEST = '@@user/AUTHENTICATE_USER_REQUEST',
  AUTHENTICATE_USER_SUCESS = '@@user/AUTHENTICATE_USER_SUCESS',
  AUTHENTICATE_USER_FAILURE = '@@user/AUTHENTICATE_USER_FAILURE'
}

export type userAuthenticateAction =
  | { type: userAuthenticateActionTypes.AUTHENTICATE_USER_REQUEST }
  | { type: userAuthenticateActionTypes.AUTHENTICATE_USER_SUCESS; payload: User }
  | { type: userAuthenticateActionTypes.AUTHENTICATE_USER_FAILURE };
