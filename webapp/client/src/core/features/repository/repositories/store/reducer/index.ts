import { combineReducers } from 'redux';

import { IRepositoriesState } from '../types';
import communications from './communications';
import data from './data';

export default combineReducers<IRepositoriesState>({
  data,
  communications,
});
