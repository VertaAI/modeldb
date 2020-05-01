import { combineReducers } from 'redux';

import { makeCommunicationReducerFromResetableAsyncAction } from 'core/shared/utils/redux/actions';

import * as actions from '../actions';
import { ICompareChangesState } from '../types';

export default combineReducers<ICompareChangesState['communications']>({
  loadingCommitPointersCommits: makeCommunicationReducerFromResetableAsyncAction(
    actions.loadCommitPointersCommits
  ),
  mergingCommits: makeCommunicationReducerFromResetableAsyncAction(
    actions.mergeCommits
  ),
});
