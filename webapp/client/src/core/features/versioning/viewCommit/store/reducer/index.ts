import { combineReducers } from 'redux';

import { IViewCommitState } from '../types';
import communications from './communications';
import data from './data';

export default combineReducers<IViewCommitState>({
  data,
  communications,
});
