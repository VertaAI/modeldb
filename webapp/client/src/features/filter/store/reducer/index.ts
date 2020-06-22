import { combineReducers } from 'redux';

import { IFilterState } from '../types';
import data from './data';

export default combineReducers<IFilterState>({
  data,
});
