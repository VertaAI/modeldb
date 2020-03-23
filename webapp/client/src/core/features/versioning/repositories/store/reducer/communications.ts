import { combineReducers } from 'redux';

import {
  makeCommunicationReducerFromResetableAsyncAction,
  makeCommunicationByIdReducerFromResetableActionCreators,
} from 'core/shared/utils/redux/actions';

import * as actions from '../actions';
import { IRepositoriesState } from '../types';

export default combineReducers<IRepositoriesState['communications']>({
  creatingRepository: makeCommunicationReducerFromResetableAsyncAction(
    actions.createRepository
  ),
  loadingRepositories: makeCommunicationReducerFromResetableAsyncAction(
    actions.loadRepositories
  ),
  loadingRepositoryByName: makeCommunicationByIdReducerFromResetableActionCreators(
    actions.loadRepositoryByName,
    {
      request: ({ name }) => name,
      success: ({ name }) => name,
      failure: ({ name }) => name,
      reset: ({ name }) => name,
    }
  ),
  deletingRepositoryById: makeCommunicationByIdReducerFromResetableActionCreators(
    actions.deleteRepository,
    {
      request: ({ id }) => id,
      success: ({ id }) => id,
      failure: ({ id }) => id,
      reset: ({ id }) => id,
    }
  ),
  addingRepositoryLabel: makeCommunicationByIdReducerFromResetableActionCreators(
    actions.addRepositoryLabel,
    {
      request: ({ repositoryId }) => repositoryId,
      success: ({ repositoryId }) => repositoryId,
      failure: ({ repositoryId }) => repositoryId,
      reset: ({ repositoryId }) => repositoryId,
    }
  ),
  deletingRepositoryLabel: makeCommunicationByIdReducerFromResetableActionCreators(
    actions.deleteRepositoryLabel,
    {
      request: ({ repositoryId }) => repositoryId,
      success: ({ repositoryId }) => repositoryId,
      failure: ({ repositoryId }) => repositoryId,
      reset: ({ repositoryId }) => repositoryId,
    }
  ),
});
