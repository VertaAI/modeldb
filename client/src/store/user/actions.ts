import { action } from 'typesafe-actions';

import { history } from 'index';
import User from 'models/User';
import ServiceFactory from 'services/ServiceFactory';
import { ActionResult } from 'store/store';

import routes from 'routes';
import {
  checkUserAuthenticationActionTypes,
  ICheckUserAuthenticationAction,
  IUserLogoutAction,
  userAuthenticateAction,
  userAuthenticateActionTypes,
  userLogoutActionTypes
} from './types';

export const authenticateUser = (): ActionResult<void, userAuthenticateAction> => async (dispatch, getState) => {
  dispatch(action(userAuthenticateActionTypes.AUTHENTICATE_USER_REQUEST));

  ServiceFactory.getAuthenticationService().login();
};

export const logoutUser = (): ActionResult<void, IUserLogoutAction> => async (dispatch, getState) => {
  dispatch(action(userLogoutActionTypes.LOGOUT_USER));

  ServiceFactory.getAuthenticationService().logout();
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
      return authenticatedService.user;
    }
    return null;
  } catch {
    return null;
  }
}
export const checkUserAuthentication = (): ActionResult<void, ICheckUserAuthenticationAction> => async (dispatch, getState) => {
  dispatch(action(checkUserAuthenticationActionTypes.CHECKING_USER_AUTH_REQUEST));

  try {
    const user = getUser();
    dispatch(action(checkUserAuthenticationActionTypes.CHECKING_USER_AUTH_SUCCESS, user));
  } catch {
    dispatch(action(checkUserAuthenticationActionTypes.CHECKING_USER_AUTH_FAILURE));
  }
};
