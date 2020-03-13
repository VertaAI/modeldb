import { combineReducers } from 'redux';

import { IDescriptionActionState } from '../types';
import communications from './communications';

export default combineReducers<IDescriptionActionState>({
  communications,
});
