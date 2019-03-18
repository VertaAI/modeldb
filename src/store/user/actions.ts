import { ActionResult } from 'store/store';
import { action } from 'typesafe-actions';
import { history } from 'index';
import ServiceFactory from 'services/ServiceFactory';
import { IUserLogoutAction, userAuthenticateAction, userAuthenticateActionTypes, userLogoutActionTypes } from './types';
import routes from '../../routes';

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
