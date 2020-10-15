import { combineReducers } from 'redux';

import { ICommentsState } from '../types';
import communications from './communications';
import data from './data';

export default combineReducers<ICommentsState>({
  data,
  communications,
});
