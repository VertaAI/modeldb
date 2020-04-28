import { ActionType, createReducer } from 'typesafe-actions';

import { defaultBranch } from 'core/shared/models/Versioning/RepositoryData';

import * as actions from '../actions';
import { IRepositoryDataState } from '../types';

const initial: IRepositoryDataState['data'] = {
  commitWithComponent: null,
  currentBlobExperimentRuns: null,

  tags: null,
  commitPointer: { type: 'branch', value: defaultBranch },
  branches: [],
};

export default createReducer<
  IRepositoryDataState['data'],
  ActionType<typeof actions>
>(initial)
  .handleAction(actions.loadCommitWithComponent.success, (state, action) => ({
    ...state,
    commitWithComponent: action.payload,
  }))
  .handleAction(
    actions.loadCurrentBlobExperimentRuns.success,
    (state, action) => ({
      ...state,
      currentBlobExperimentRuns: action.payload,
    })
  )
  .handleAction(actions.loadCommitWithComponent.failure, (state, action) => ({
    ...state,
    commitWithComponent: null,
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
