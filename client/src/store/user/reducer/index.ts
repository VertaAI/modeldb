import { combineReducers } from 'redux';

import { IUserState } from '../types';
import communications from './communications';
import data from './data';

export default combineReducers<IUserState>({
  data,
  communications,
});
