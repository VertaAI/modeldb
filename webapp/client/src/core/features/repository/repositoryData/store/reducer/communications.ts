import { combineReducers } from 'redux';

import { makeCommunicationReducerFromResetableAsyncAction } from 'core/shared/utils/redux/actions';

import * as actions from '../actions';
import { IRepositoryDataState } from '../types';

export default combineReducers<IRepositoryDataState['communications']>({
  loadingCommitWithData: makeCommunicationReducerFromResetableAsyncAction(
    actions.loadCommitWithData
  ),

  loadingTags: makeCommunicationReducerFromResetableAsyncAction(
    actions.loadTags
  ),

  loadingBranches: makeCommunicationReducerFromResetableAsyncAction(
    actions.loadBranches
  ),
});
