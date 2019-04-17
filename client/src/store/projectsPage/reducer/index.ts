import { combineReducers } from 'redux';

import { IProjectsPageState } from '../types';
import data from './data';

export default combineReducers<IProjectsPageState>({
  data,
});
