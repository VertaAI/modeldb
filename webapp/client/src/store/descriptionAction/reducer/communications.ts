import { combineReducers } from 'redux';

import { makeCommunicationReducerFromEnum } from 'core/shared/utils/redux/communication';

import { IDescriptionActionState, addOrEditDescActionTypes } from '../types';

export default combineReducers<IDescriptionActionState['communications']>({
  editingDesc: makeCommunicationReducerFromEnum(addOrEditDescActionTypes),
  addingDesc: makeCommunicationReducerFromEnum(addOrEditDescActionTypes),
});
