import { combineReducers } from 'redux';

import { IDeployState } from '../types';
import communications from './communications';
import data from './data';

export default combineReducers<IDeployState>({
  data,
  communications,
});
