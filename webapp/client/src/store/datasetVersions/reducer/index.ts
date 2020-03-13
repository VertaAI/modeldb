import { combineReducers } from 'redux';

import { IDatasetVersionsState } from '../types';
import communications from './communications';
import data from './data';

export default combineReducers<IDatasetVersionsState>({
  data,
  communications,
});
