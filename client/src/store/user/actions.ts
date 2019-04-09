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
  dispatch(action(authenticateUserActionTypes.REQUEST));

  ServiceFactory.getAuthenticationService().login();
};

export const logoutUser = (): ActionResult<
  void,
  ILogoutActions
> => async dispatch => {
  dispatch(action(logoutActionTypes.REQUEST));

  await ServiceFactory.getAuthenticationService().logout();
  dispatch(action(logoutActionTypes.SUCCESS));
};

export const checkUserAuthentication = (): ActionResult<
  void,
  ICheckUserAuthenticationActions
> => async dispatch => {
  dispatch(action(checkUserAuthenticationActionTypes.REQUEST));

  try {
    const user = await ServiceFactory.getAuthenticationService().loadUser();
    dispatch(action(checkUserAuthenticationActionTypes.SUCCESS, user));
  } catch {
    dispatch(action(checkUserAuthenticationActionTypes.FAILURE, 'error'));
  }
};
