import { ActionResult } from 'store/store';
import { action } from 'typesafe-actions';
import ServiceFactory from '../../services/ServiceFactory';
import { IUserLogoutAction, userAuthenticateAction, userAuthenticateActionTypes, userLogoutActionTypes } from './types';

export const authenticateUser = (): ActionResult<void, userAuthenticateAction> => async (dispatch, getState) => {
  dispatch(action(userAuthenticateActionTypes.AUTHENTICATE_USER_REQUEST));

  await ServiceFactory.getAuthenticationService()
    .authenticate()
    .then(res => {
      localStorage.setItem('user', JSON.stringify(res));
      dispatch(action(userAuthenticateActionTypes.AUTHENTICATE_USER_SUCESS, res));
    });
};

export const logoutUser = (): ActionResult<void, IUserLogoutAction> => async (dispatch, getState) => {
  dispatch(action(userLogoutActionTypes.LOGOUT_USER));

  ServiceFactory.getAuthenticationService().logout();
  localStorage.removeItem('user');
};
