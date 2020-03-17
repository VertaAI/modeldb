import { combineReducers } from 'redux';

import { makeCommunicationReducerFromResetableAsyncAction } from 'core/shared/utils/redux/actions';

import * as actions from '../actions';
import { ICommitsHistoryState } from '../types';

export default combineReducers<ICommitsHistoryState['communications']>({
  loadingCommits: makeCommunicationReducerFromResetableAsyncAction(
    actions.loadCommits
  ),
});
