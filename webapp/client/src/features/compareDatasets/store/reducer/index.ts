import { combineReducers } from 'redux';

import { ICompareDatasetsState } from '../types';
import data from './data';

export default combineReducers<ICompareDatasetsState>({
  data,
});
