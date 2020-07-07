import { combineReducers } from 'redux';

import { IHighLevelSearchState } from '../types';
import data from './data';

export default combineReducers<IHighLevelSearchState>({
  data,
});
