import { combineReducers } from 'redux';

import { IProjectsState } from '../types';
import communications from './communications';
import data from './data';

export default combineReducers<IProjectsState>({
  data,
  communications,
});
