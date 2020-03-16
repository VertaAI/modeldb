import { combineReducers } from 'redux';

import { ICompareCommitsState } from '../types';
import communications from './communications';
import data from './data';

export default combineReducers<ICompareCommitsState>({
  data,
  communications,
});
