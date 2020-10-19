import { combineReducers } from 'redux';

import { IExperimentRunsState } from '../types';
import communications from './communications';
import data from './data';

export default combineReducers<IExperimentRunsState>({
  data,
  communications,
});
