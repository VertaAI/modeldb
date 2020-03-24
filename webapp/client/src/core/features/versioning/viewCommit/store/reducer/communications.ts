import { combineReducers } from 'redux';

import { makeCommunicationReducerFromResetableAsyncAction } from 'core/shared/utils/redux/actions';

import * as actions from '../actions';
import { IViewCommitState } from '../types';

export default combineReducers<IViewCommitState['communications']>({
  loadingCommit: makeCommunicationReducerFromResetableAsyncAction(
    actions.loadCommit
  ),
  loadingCommitExperimentRuns: makeCommunicationReducerFromResetableAsyncAction(
    actions.loadCommitExperimentRuns
  ),
});
