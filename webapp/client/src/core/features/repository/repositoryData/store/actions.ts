import { createAction } from 'typesafe-actions';

import { AppError } from 'core/shared/models/Error';
import { IRepository } from 'core/shared/models/Repository/Repository';
import {
  IDataRequest,
  ICommitWithData,
} from 'core/shared/models/Repository/RepositoryData';
import {
  CommitTag,
  Branch,
  CommitPointer,
} from 'core/shared/models/Repository/RepositoryData';
import * as Actions from 'utils/redux/actions';

export const loadCommitWithData = Actions.makeThunkApiRequest(
  '@@repositoryData/LOAD_COMMIT_WITH_DATA_REQUEST',
  '@@repositoryData/LOAD_COMMIT_WITH_DATA_SUCCESS',
  '@@repositoryData/LOAD_COMMIT_WITH_DATA_FAILURE',
  '@@repositoryData/LOAD_COMMIT_WITH_DATA_RESET'
)<IDataRequest, ICommitWithData, AppError, undefined>(
  async ({ payload, dependencies: { ServiceFactory } }) => {
    return await ServiceFactory.getRepositoryDataService().loadCommitWithData(
      payload
    );
  }
);

export const loadTags = Actions.makeThunkApiRequest(
  '@@repositoryData/LOAD_TAGS_REQUEST',
  '@@repositoryData/LOAD_TAGS_SUCCESS',
  '@@repositoryData/LOAD_TAGS_FAILURE',
  '@@repositoryData/LOAD_TAGS_RESET'
)<{ repositoryId: IRepository['id'] }, CommitTag[], AppError, undefined>(
  async ({ dependencies: { ServiceFactory }, payload }) => {
    return await ServiceFactory.getRepositoryDataService().loadTags(payload);
  }
);
export const changeCommitPointer = createAction(
  '@@repositoryData/CHANGE_COMMIT_POINTER'
)<CommitPointer>();

export const loadBranches = Actions.makeThunkApiRequest(
  '@@repositoryData/LOAD_BRANCHES_REQUEST',
  '@@repositoryData/LOAD_BRANCHES_SUCCESS',
  '@@repositoryData/LOAD_BRANCHES_FAILURE',
  '@@repositoryData/LOAD_BRANCHES_RESET'
)<{ repositoryId: IRepository['id'] }, Branch[], AppError, undefined>(
  async ({ payload, dependencies: { ServiceFactory } }) => {
    return await ServiceFactory.getRepositoryDataService().loadBranches(
      payload.repositoryId
    );
  }
);

export const resetFeatureState = createAction(
  '@@repositoryData/RESET_FEATURE_STATE'
)();
