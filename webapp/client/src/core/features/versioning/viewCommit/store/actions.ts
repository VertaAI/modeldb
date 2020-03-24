import { UnavailableEntityApiErrorType } from 'core/services/shared/UnavailableEntityApiError';
import { AppError } from 'core/shared/models/Error';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import {
  ICommit,
  IHydratedCommit,
} from 'core/shared/models/Versioning/RepositoryData';
import * as Actions from 'utils/redux/actions';
import { IExperimentRunInfo } from 'models/ModelRecord';
import { selectCurrentWorkspaceName } from 'store/workspaces';

export const loadCommit = Actions.makeThunkApiRequest(
  '@@viewCommit/LOAD_COMMIT_REQUEST',
  '@@viewCommit/LOAD_COMMIT_SUCCESS',
  '@@viewCommit/LOAD_COMMIT_FAILURE',
  '@@viewCommit/LOAD_COMMIT_RESET'
)<
  { repositoryId: IRepository['id']; commitSha: ICommit['sha'] },
  IHydratedCommit,
  AppError<UnavailableEntityApiErrorType>
>(async ({ payload, dependencies: { ServiceFactory }, getState }) => {
  return await ServiceFactory.getRepositoryDataService().loadCommit(payload);
});

export const loadCommitExperimentRuns = Actions.makeThunkApiRequest(
  '@@viewCommit/LOAD_COMMIT_EXPERIMENT_RUNS_REQUEST',
  '@@viewCommit/LOAD_COMMIT_EXPERIMENT_RUNS_SUCCESS',
  '@@viewCommit/LOAD_COMMIT_EXPERIMENT_RUNS_FAILURE',
  '@@viewCommit/LOAD_COMMIT_EXPERIMENT_RUNS_RESET'
)<
  { repositoryId: IRepository['id']; commitSha: ICommit['sha'] },
  IExperimentRunInfo[],
  AppError
>(async ({ payload, dependencies: { ServiceFactory }, getState }) => {
  return await ServiceFactory.getRepositoryDataService().loadCommitExperimentRuns(
    {
      ...payload,
      workspaceName: selectCurrentWorkspaceName(getState()),
    }
  );
});
