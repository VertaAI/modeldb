import User from 'models/User';
import {
  ICommunication,
  MakeCommunicationActions,
  makeCommunicationActionTypes,
} from 'utils/redux/communication';

export interface IUserState {
  data: {
    user: User | null;
    authenticated: boolean;
  };
  communications: {
    checkingUserAuthentication: ICommunication;
    authenticatingUser: ICommunication;
    logouting: ICommunication;
  };
}

export const authenticateUserActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@user/AUTHENTICATE_USER_REQUEST',
  SUCCESS: '@@user/AUTHENTICATE_USER_SUCСESS',
  FAILURE: '@@user/AUTHENTICATE_USER_FAILURE',
});
export type IAuthenticateUserActions = MakeCommunicationActions<
  typeof authenticateUserActionTypes,
  { success: User }
>;

export const checkUserAuthenticationActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@user/CHECK_USER_AUTHENTICATION_REQUEST',
  SUCCESS: '@@user/CHECK_USER_AUTHENTICATION_SUCСESS',
  FAILURE: '@@user/CHECK_USER_AUTHENTICATION_FAILURE',
});
export type ICheckUserAuthenticationActions = MakeCommunicationActions<
  typeof checkUserAuthenticationActionTypes,
  { success: User | null }
>;

export const logoutActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@user/LOGOUT_REQUEST',
  SUCCESS: '@@user/LOGOUT_SUCСESS',
  FAILURE: '@@user/LOGOUT_FAILURE',
});
export type ILogoutActions = MakeCommunicationActions<
  typeof logoutActionTypes,
  {}
>;

export type FeatureAction =
  | IAuthenticateUserActions
  | ICheckUserAuthenticationActions
  | ILogoutActions;
