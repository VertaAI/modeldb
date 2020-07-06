import { combineReducers } from 'redux';

import { ITagsManagerState } from '../types';
import communications from './communications';

export default combineReducers<ITagsManagerState>({
  communications,
});
