import { combineReducers } from 'redux';

import { makeCommunicationReducerFromEnum } from 'shared/utils/redux/communication';

import { IDescriptionManagerState, addOrEditDescActionTypes } from '../types';

export default combineReducers<IDescriptionManagerState['communications']>({
  editingDesc: makeCommunicationReducerFromEnum(addOrEditDescActionTypes),
  addingDesc: makeCommunicationReducerFromEnum(addOrEditDescActionTypes),
});
