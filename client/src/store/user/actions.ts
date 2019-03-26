import { action } from 'typesafe-actions';

import { ActionResult } from 'store/store';
import ServiceFactory from 'services/ServiceFactory';

import {
  IUserLogoutAction,
  userAuthenticateAction,
  userAuthenticateActionTypes,
  userLogoutActionTypes,
  ICheckUserAuthenticationAction,
  checkUserAuthenticationActionTypes
} from './types';

export const authenticateUser = (): ActionResult<void, userAuthenticateAction> => async (dispatch, getState) => {
  dispatch(action(userAuthenticateActionTypes.AUTHENTICATE_USER_REQUEST));

  ServiceFactory.getAuthenticationService().login();
};

export const logoutUser = (): ActionResult<void, IUserLogoutAction> => async (dispatch, getState) => {
  dispatch(action(userLogoutActionTypes.LOGOUT_USER));

  await ServiceFactory.getAuthenticationService().logout();
};

export const checkUserAuthentication = (): ActionResult<void, ICheckUserAuthenticationAction> => async (dispatch, getState) => {
  dispatch(action(checkUserAuthenticationActionTypes.CHECKING_USER_AUTH_REQUEST));

  try {
    const user = await ServiceFactory.getAuthenticationService().loadUser();
    dispatch(action(checkUserAuthenticationActionTypes.CHECKING_USER_AUTH_SUCCESS, user));
  } catch {
    dispatch(action(checkUserAuthenticationActionTypes.CHECKING_USER_AUTH_FAILURE));
  }
};
