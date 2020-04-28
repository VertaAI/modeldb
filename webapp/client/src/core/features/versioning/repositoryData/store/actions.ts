import { createAction } from 'typesafe-actions';

import { AppError } from 'core/shared/models/Error';
import { CommitComponentLocation } from 'core/shared/models/Versioning/CommitComponentLocation';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import {
  ICommitComponentRequest,
  ICommitWithComponent,
  ICommit,
  CommitTag,
  Branch,
  CommitPointer,
} from 'core/shared/models/Versioning/RepositoryData';
import { IExperimentRunInfo } from 'models/ModelRecord';
import { selectCurrentWorkspaceName } from 'store/workspaces';
import * as Actions from 'utils/redux/actions';

export const loadCommitWithComponent = Actions.makeThunkApiRequest(
  '@@repositoryData/LOAD_COMMIT_WITH_COMPONENT_REQUEST',
  '@@repositoryData/LOAD_COMMIT_WITH_COMPONENT_SUCCESS',
  '@@repositoryData/LOAD_COMMIT_WITH_COMPONENT_FAILURE',
  '@@repositoryData/LOAD_COMMIT_WITH_COMPONENT_RESET'
)<ICommitComponentRequest, ICommitWithComponent, AppError, undefined>(
  async ({ payload, dependencies: { ServiceFactory } }) => {
    return await ServiceFactory.getRepositoryDataService().loadCommitWithComponent(
      payload
    );
  }
);

export const loadCurrentBlobExperimentRuns = Actions.makeThunkApiRequest(
  '@@repositoryData/LOAD_BLOB_EXPERIMENT_RUNS_REQUEST',
  '@@repositoryData/LOAD_BLOB_EXPERIMENT_RUNS_SUCCESS',
  '@@repositoryData/LOAD_BLOB_EXPERIMENT_RUNS_FAILURE',
  '@@repositoryData/LOAD_BLOB_EXPERIMENT_RUNS_RESET'
)<
  {
    repositoryId: IRepository['id'];
    commitSha: ICommit['sha'];
    location: CommitComponentLocation;
  },
  IExperimentRunInfo[],
  AppError,
  undefined
>(async ({ payload, dependencies: { ServiceFactory }, getState }) => {
  return await ServiceFactory.getRepositoryDataService().loadBlobExperimentRuns(
    {
      ...payload,
      workspaceName: selectCurrentWorkspaceName(getState()),
    }
  );
});

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
