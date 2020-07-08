import { combineReducers } from 'redux';

import { ICompareEntitiesState } from '../types';
import data from './data';

export default combineReducers<ICompareEntitiesState>({
  data,
});
