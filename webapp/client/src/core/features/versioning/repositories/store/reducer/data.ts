import { ActionType, createReducer } from 'typesafe-actions';

import { substractPaginationTotalCount } from 'core/shared/models/Pagination';

import * as actions from '../actions';
import { IRepositoriesState } from '../types';
import update from 'ramda/es/update';
import { updateById } from 'core/shared/utils/collection';

const initial: IRepositoriesState['data'] = {
  repositories: null,
  pagination: {
    currentPage: 0,
    pageSize: 10,
    totalCount: 0,
  },
};

export default createReducer<
  IRepositoriesState['data'],
  ActionType<typeof actions>
>(initial)
  .handleAction(actions.createRepository.success, (state, action) => ({
    ...state,
    repositories: (state.repositories || []).concat(action.payload),
  }))
  .handleAction(actions.loadRepositories.success, (state, action) => ({
    ...state,
    repositories: action.payload.data,
    pagination: {
      ...state.pagination,
      totalCount: action.payload.totalCount,
    },
  }))
  .handleAction(actions.deleteRepository.success, (state, action) => ({
    ...state,
    repositories: (state.repositories || []).filter(
      r => r.id !== action.payload.id
    ),
    pagination: substractPaginationTotalCount(1, state.pagination),
  }))
  .handleAction(actions.loadRepositoryByName.success, (state, action) => ({
    ...state,
    repositories: (state.repositories || []).concat(action.payload),
  }))
  .handleAction(actions.changeCurrentPage, (state, action) => ({
    ...state,
    pagination: {
      ...state.pagination,
      currentPage: action.payload,
    },
  }))
  .handleAction(actions.addRepositoryLabel.success, (state, action) => ({
    ...state,
    repositories: updateById(
      repo => ({
        ...repo,
        labels: [...repo.labels, action.payload.label],
      }),
      action.payload.repositoryId,
      state.repositories || []
    ),
  }))
  .handleAction(actions.deleteRepositoryLabel.success, (state, action) => ({
    ...state,
    repositories: updateById(
      repo => ({
        ...repo,
        labels: repo.labels.filter(l => l !== action.payload.label),
      }),
      action.payload.repositoryId,
      state.repositories || []
    ),
  }));
