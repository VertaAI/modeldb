import { combineReducers } from 'redux';

import { IProjectCreationState } from '../types';
import communications from './communications';

export default combineReducers<IProjectCreationState>({
  communications,
});
