import { combineReducers } from 'redux';

import { ICommitsHistoryState } from '../types';
import communications from './communications';
import data from './data';

export default combineReducers<ICommitsHistoryState>({
  data,
  communications,
});
