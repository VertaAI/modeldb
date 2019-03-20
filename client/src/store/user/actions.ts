import { ActionResult } from 'store/store';
import { action } from 'typesafe-actions';
import jwtDecode from 'jwt-decode';
import { history } from '../../index';
import ServiceFactory from '../../services/ServiceFactory';
import {
  IUserLogoutAction,
  userAuthenticateAction,
  userAuthenticateActionTypes,
  userLogoutActionTypes,
  ICheckUserAuthenticationAction,
  checkUserAuthenticationActionTypes
} from './types';
import routes from '../../routes';
import User from '../../models/User';

export const authenticateUser = (): ActionResult<void, userAuthenticateAction> => async (dispatch, getState) => {
  dispatch(action(userAuthenticateActionTypes.AUTHENTICATE_USER_REQUEST));

  ServiceFactory.getAuthenticationService().login();
};

export const logoutUser = (): ActionResult<void, IUserLogoutAction> => async (dispatch, getState) => {
  dispatch(action(userLogoutActionTypes.LOGOUT_USER));

  await ServiceFactory.getAuthenticationService().logout();
};

export const handleUserAuthentication = (): ActionResult<void, userAuthenticateAction> => async (dispatch, getState) => {
  try {
    const authenticationService = ServiceFactory.getAuthenticationService();

    await authenticationService.handleAuthentication();
    const profile = await authenticationService.getProfile();
    history.push(routes.mainPage.getRedirectPath({}));
    dispatch(action(userAuthenticateActionTypes.AUTHENTICATE_USER_SUCCESS, profile));
  } catch (error) {
    dispatch(action(userAuthenticateActionTypes.AUTHENTICATE_USER_FAILURE));
  }
};

function getUser(): User | null {
  try {
    const authenticatedService = ServiceFactory.getAuthenticationService();
    if (authenticatedService.authenticated) {
      return jwtDecode<User>(authenticatedService.idToken);
    }
    return null;
  } catch {
    return null;
  }
}
export const checkUserAuthentication = (): ActionResult<void, ICheckUserAuthenticationAction> => async (dispatch, getState) => {
  dispatch(action(checkUserAuthenticationActionTypes.CHECKING_USER_AUTH_REQUEST));

  try {
    const user = await ServiceFactory.getAuthenticationService().loadUser();
    console.log('user', user);
    dispatch(action(checkUserAuthenticationActionTypes.CHECKING_USER_AUTH_SUCCESS, user));
  } catch {
    dispatch(action(checkUserAuthenticationActionTypes.CHECKING_USER_AUTH_FAILURE));
  }
};
