import { ActionResult } from 'store/store';
import { action } from 'typesafe-actions';
import ServiceFactory from '../../services/ServiceFactory';
import { userAuthenticateAction, userAuthenticateActionTypes } from './types';

export const authenticateUser = (): ActionResult<void, userAuthenticateAction> => async (dispatch, getState) => {
  dispatch(action(userAuthenticateActionTypes.AUTHENTICATE_USER_REQUEST));

  await ServiceFactory.getAuthenticationService()
    .authenticate()
    .then(res => {
      localStorage.setItem('user', JSON.stringify(res));
      dispatch(action(userAuthenticateActionTypes.AUTHENTICATE_USER_SUCESS, res));
    });
};
