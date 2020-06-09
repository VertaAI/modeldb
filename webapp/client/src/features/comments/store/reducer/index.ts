import { combineReducers } from 'redux';

import { IComment } from '../../../../core/shared/models/Comment';
import { ICommentsState } from '../types';
import communications from './communications';
import data from './data';

export default combineReducers<ICommentsState>({
  data,
  communications,
});
