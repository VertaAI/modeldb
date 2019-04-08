import { action } from 'typesafe-actions';

import ServiceFactory from 'services/ServiceFactory';
import { ActionResult } from 'store/store';

import {
  authenticateUserActionTypes,
  checkUserAuthenticationActionTypes,
  IAuthenticateUserActions,
  ICheckUserAuthenticationActions,
  ILogoutActions,
  logoutActionTypes,
} from './types';

export const authenticateUser = (): ActionResult<
  void,
  IAuthenticateUserActions
> => async dispatch => {
  dispatch(action(authenticateUserActionTypes.request));

  ServiceFactory.getAuthenticationService().login();
};

export const logoutUser = (): ActionResult<
  void,
  ILogoutActions
> => async dispatch => {
  dispatch(action(logoutActionTypes.request));

  await ServiceFactory.getAuthenticationService().logout();
  dispatch(action(logoutActionTypes.success));
};

export const checkUserAuthentication = (): ActionResult<
  void,
  ICheckUserAuthenticationActions
> => async dispatch => {
  dispatch(action(checkUserAuthenticationActionTypes.request));

  try {
    const user = await ServiceFactory.getAuthenticationService().loadUser();
    dispatch(action(checkUserAuthenticationActionTypes.success, user));
  } catch {
    dispatch(action(checkUserAuthenticationActionTypes.failure, 'error'));
  }
};
