import { combineReducers } from 'redux';

import { makeCommunicationReducerFromEnum } from 'utils/redux/communication';

import { IModelRecordState, loadModelRecordActionTypes } from '../types';

export default combineReducers<IModelRecordState['communications']>({
  loadingModelRecord: makeCommunicationReducerFromEnum(
    loadModelRecordActionTypes
  ),
});
