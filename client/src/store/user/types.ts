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
  request: '@@user/AUTHENTICATE_USER_REQUEST',
  success: '@@user/AUTHENTICATE_USER_SUCСESS',
  failure: '@@user/AUTHENTICATE_USER_FAILURE',
});
export type IAuthenticateUserActions = MakeCommunicationActions<
  typeof authenticateUserActionTypes,
  { success: User }
>;

export const checkUserAuthenticationActionTypes = makeCommunicationActionTypes({
  request: '@@user/CHECK_USER_AUTHENTICATION_REQUEST',
  success: '@@user/CHECK_USER_AUTHENTICATION_SUCСESS',
  failure: '@@user/CHECK_USER_AUTHENTICATION_FAILURE',
});
export type ICheckUserAuthenticationActions = MakeCommunicationActions<
  typeof checkUserAuthenticationActionTypes,
  { success: User | null }
>;

export const logoutActionTypes = makeCommunicationActionTypes({
  request: '@@user/LOGOUT_REQUEST',
  success: '@@user/LOGOUT_SUCСESS',
  failure: '@@user/LOGOUT_FAILURE',
});
export type ILogoutActions = MakeCommunicationActions<
  typeof logoutActionTypes,
  {}
>;

export type FeatureAction =
  | IAuthenticateUserActions
  | ICheckUserAuthenticationActions
  | ILogoutActions;
