import { combineReducers } from 'redux';

import { IArtifactManagerState } from '../types';
import communications from './communications';
import data from './data';

export default combineReducers<IArtifactManagerState>({
  data,
  communications,
});
