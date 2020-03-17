import { combineReducers } from 'redux';

import { ICompareChangesState } from '../types';
import communications from './communications';
import data from './data';

export default combineReducers<ICompareChangesState>({
  data,
  communications,
});
