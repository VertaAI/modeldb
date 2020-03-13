import { combineReducers } from 'redux';

import { makeCommunicationReducerFromEnum } from 'core/shared/utils/redux/communication';

import {
  ITagActionState,
  addTagActionTypes,
  removeTagActionTypes,
} from '../types';

export default combineReducers<ITagActionState['communications']>({
  addingTag: makeCommunicationReducerFromEnum(addTagActionTypes),
  removingTag: makeCommunicationReducerFromEnum(removeTagActionTypes),
});
