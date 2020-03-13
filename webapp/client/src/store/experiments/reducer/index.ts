import { combineReducers } from 'redux';

import { IExperimentsState } from '../types';
import communications from './communications';
import data from './data';

export default combineReducers<IExperimentsState>({
  data,
  communications,
});
