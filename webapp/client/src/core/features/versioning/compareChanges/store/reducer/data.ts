import { createReducer, ActionType } from 'typesafe-actions';

import * as actions from '../actions';
import { ICompareChangesState } from '../types';

const initial: ICompareChangesState['data'] = {
  commitPointersCommits: null,
};

export default createReducer<
  ICompareChangesState['data'],
  ActionType<typeof actions>
>(initial).handleAction(
  actions.loadCommitPointersCommits.success,
  (state, action) => ({
    ...state,
    commitPointersCommits: action.payload,
  })
);
