import { combineReducers } from 'redux';

import { IComment } from '../../Model';
import { ICommentsState } from '../types';
import communications from './communications';
import data from './data';

export default combineReducers<ICommentsState<IComment>>({
  data,
  communications,
});
