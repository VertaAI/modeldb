import { Reducer } from 'redux';

import {
  authenticateUserActionTypes,
  checkUserAuthenticationActionTypes,
  FeatureAction,
  IUserState,
  logoutActionTypes,
} from '../types';

const initial: IUserState['data'] = {
  authenticated: false,
  user: null,
};

const dataReducer: Reducer<IUserState['data'], FeatureAction> = (
  state = initial,
  action
) => {
  switch (action.type) {
    case logoutActionTypes.REQUEST: {
      return { ...state, user: null, authenticated: false };
    }
    case authenticateUserActionTypes.SUCCESS: {
      return { ...state, authenticated: true, user: action.payload };
    }
    case checkUserAuthenticationActionTypes.SUCCESS: {
      return {
        ...state,
        authenticated: Boolean(action.payload),
        user: action.payload,
      };
    }
    default:
      return state;
  }
};

export default dataReducer;
