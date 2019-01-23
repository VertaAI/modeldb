import { Reducer } from 'redux';
import { IUserLogoutAction, IUserState, userAuthenticateAction, userAuthenticateActionTypes, userLogoutActionTypes } from './types';

const initialState: IUserState = {
  user: null
};

const userAuthenticateReducer: Reducer<IUserState> = (state = initialState, action: userAuthenticateAction) => {
  switch (action.type) {
    case userAuthenticateActionTypes.AUTHENTICATE_USER_REQUEST: {
      return { ...state };
    }
    case userAuthenticateActionTypes.AUTHENTICATE_USER_SUCESS: {
      return { ...state, user: action.payload };
    }
    default: {
      return state;
    }
  }
};

const userLogoutReducer: Reducer<IUserState> = (state = initialState, action: IUserLogoutAction) => {
  switch (action.type) {
    case userLogoutActionTypes.LOGOUT_USER: {
      return { ...state, user: null };
    }
    default: {
      return state;
    }
  }
};

export const userReducer: Reducer<IUserState> = (state = initialState, action) => {
  if (Object.values(userAuthenticateActionTypes).includes(action.type)) {
    return userAuthenticateReducer(state, action);
  }
  if (Object.values(userLogoutActionTypes).includes(action.type)) {
    return userLogoutReducer(state, action);
  }
  return state;
};
