import { combineReducers } from 'redux';

import { makeCommunicationReducerFromResetableAsyncAction } from 'core/shared/utils/redux/actions';

import * as actions from '../actions';
import { ICompareCommitsState } from '../types';

export default combineReducers<ICompareCommitsState['communications']>({
  loadingCommitsDiff: makeCommunicationReducerFromResetableAsyncAction(
    actions.loadCommitsDiff
  ),
});
