import { ActionType, createReducer } from 'typesafe-actions';

import * as actions from '../actions';
import { ICommitsHistoryState } from '../types';

const initial: ICommitsHistoryState['data'] = {
  commitsWithPagination: null,
};

export default createReducer<
  ICommitsHistoryState['data'],
  ActionType<typeof actions>
>(initial).handleAction(actions.loadCommits.success, (state, action) => ({
  ...state,
  commitsWithPagination: action.payload,
}));
