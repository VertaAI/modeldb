import { combineReducers } from 'redux';

import { IDatasetsState } from '../types';
import communications from './communications';
import data from './data';

export default combineReducers<IDatasetsState>({
  data,
  communications,
});
