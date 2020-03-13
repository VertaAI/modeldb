import { combineReducers } from 'redux';

import { ITagActionState } from '../types';
import communications from './communications';

export default combineReducers<ITagActionState>({
  communications,
});
