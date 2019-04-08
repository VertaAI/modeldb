import { action } from 'typesafe-actions';

import ServiceFactory from 'services/ServiceFactory';
import { ActionResult } from 'store/store';

import {
  checkUserAuthenticationActionTypes,
  ICheckUserAuthenticationAction,
  checkingUserAuthentication,
  IUserLogoutAction,
  userAuthenticateAction,
  userAuthenticateActionTypes,
  userLogoutActionTypes,
  _checkingUserAuthenticationActionTypes,
} from './types';
import { RecordValues } from 'utils/types';

export const authenticateUser = (): ActionResult<
  void,
  userAuthenticateAction
> => async dispatch => {
  dispatch(action(userAuthenticateActionTypes.AUTHENTICATE_USER_REQUEST));

  ServiceFactory.getAuthenticationService().login();
};

export const logoutUser = (): ActionResult<
  void,
  IUserLogoutAction
> => async dispatch => {
  dispatch(action(userLogoutActionTypes.LOGOUT_USER));

  await ServiceFactory.getAuthenticationService().logout();
};

export const checkUserAuthentication = (): ActionResult<
  void,
  RecordValues<checkingUserAuthentication>
> => async dispatch => {
  dispatch(action(_checkingUserAuthenticationActionTypes.request));

  try {
    const user = await ServiceFactory.getAuthenticationService().loadUser();
    dispatch(action(_checkingUserAuthenticationActionTypes.success, user));
  } catch {
    dispatch(action(_checkingUserAuthenticationActionTypes.failure, 'error'));
  }
};
