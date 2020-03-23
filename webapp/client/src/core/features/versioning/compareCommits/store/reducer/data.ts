import { createReducer, ActionType } from 'typesafe-actions';

import * as actions from '../actions';
import { ICompareCommitsState } from '../types';

const initial: ICompareCommitsState['data'] = {
  diffs: null,
};

export default createReducer<
  ICompareCommitsState['data'],
  ActionType<typeof actions>
>(initial).handleAction(actions.loadCommitsDiff.success, (state, action) => ({
  ...state,
  diffs: action.payload,
}));
