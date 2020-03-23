import { ActionType, createReducer } from 'typesafe-actions';

import { defaultBranch } from 'core/shared/models/Versioning/RepositoryData';

import * as actions from '../actions';
import { IRepositoryDataState } from '../types';

const initial: IRepositoryDataState['data'] = {
  commitWithData: null,
  tags: null,
  commitPointer: { type: 'branch', value: defaultBranch },
  branches: [],
};

export default createReducer<
  IRepositoryDataState['data'],
  ActionType<typeof actions>
>(initial)
  .handleAction(actions.loadCommitWithData.success, (state, action) => ({
    ...state,
    commitWithData: action.payload,
  }))
  .handleAction(actions.loadCommitWithData.failure, (state, action) => ({
    ...state,
    commitWithData: null,
  }))
  .handleAction(actions.loadTags.success, (state, action) => ({
    ...state,
    tags: action.payload,
  }))
  .handleAction(actions.changeCommitPointer, (state, action) => ({
    ...state,
    commitPointer: action.payload,
  }))
  .handleAction(actions.loadBranches.success, (state, action) => ({
    ...state,
    branches: action.payload,
  }));
