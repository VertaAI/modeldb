import { combineReducers } from 'redux';

import { IDescriptionManagerState } from '../types';
import communications from './communications';

export default combineReducers<IDescriptionManagerState>({
  communications,
});
