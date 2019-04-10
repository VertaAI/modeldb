import { combineReducers } from 'redux';

import { IFilterState } from '../types';
import communications from './communications';
import data from './data';

export default combineReducers<IFilterState>({
  data,
  communications,
});
