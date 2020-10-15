import { combineReducers } from 'redux';

import { ICompareModelsState } from '../types';
import data from './data';

export default combineReducers<ICompareModelsState>({
  data,
});
