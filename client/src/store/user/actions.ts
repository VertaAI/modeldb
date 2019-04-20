import { action } from 'typesafe-actions';

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
> => async (dispatch, _, { ServiceFactory }) => {
  dispatch(action(authenticateUserActionTypes.REQUEST));

  ServiceFactory.getAuthenticationService().login();
};

export const logoutUser = (): ActionResult<void, ILogoutActions> => async (
  dispatch,
  _,
  { ServiceFactory }
) => {
  dispatch(action(logoutActionTypes.REQUEST));

  await ServiceFactory.getAuthenticationService().logout();
  dispatch(action(logoutActionTypes.SUCCESS));
};

export const checkUserAuthentication = (): ActionResult<
  void,
  ICheckUserAuthenticationActions
> => async (dispatch, _, { ServiceFactory }) => {
  dispatch(action(checkUserAuthenticationActionTypes.REQUEST));

  try {
    const user = await ServiceFactory.getAuthenticationService().loadUser();
    dispatch(action(checkUserAuthenticationActionTypes.SUCCESS, user));
  } catch (e) {
    dispatch(action(checkUserAuthenticationActionTypes.FAILURE, e as string));
  }
};
