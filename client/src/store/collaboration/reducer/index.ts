import { combineReducers } from 'redux';

import { ICollaborationState } from '../types';
import communications from './communications';

export default combineReducers<ICollaborationState>({
  communications,
});
