import { combineReducers } from 'redux';

import { makeCommunicationReducerFromEnum } from 'core/shared/utils/redux/communication';

import {
  ITagsManagerState,
  addTagActionTypes,
  removeTagActionTypes,
} from '../types';

export default combineReducers<ITagsManagerState['communications']>({
  addingTag: makeCommunicationReducerFromEnum(addTagActionTypes),
  removingTag: makeCommunicationReducerFromEnum(removeTagActionTypes),
});
