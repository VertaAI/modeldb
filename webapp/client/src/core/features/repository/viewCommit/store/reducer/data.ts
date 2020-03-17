import { ActionType, createReducer } from 'typesafe-actions';

import * as actions from '../actions';
import { IViewCommitState } from '../types';

const initial: IViewCommitState['data'] = {
  commit: null,
};

export default createReducer<
  IViewCommitState['data'],
  ActionType<typeof actions>
>(initial).handleAction(actions.loadCommit.success, (state, action) => ({
  ...state,
  commit: action.payload,
}));
