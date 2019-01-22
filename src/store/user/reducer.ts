import { Reducer } from 'redux';
import { IUserState, userAuthenticateAction, userAuthenticateActionTypes } from './types';

const initialState: IUserState = {
  user: null
};

const reducer: Reducer<IUserState> = (state = initialState, action: userAuthenticateAction) => {
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

export { reducer as userReducer };
