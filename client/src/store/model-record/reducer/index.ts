import { combineReducers } from 'redux';

import { IModelRecordState } from '../types';
import communications from './communications';
import data from './data';

export default combineReducers<IModelRecordState>({
  data,
  communications,
});
