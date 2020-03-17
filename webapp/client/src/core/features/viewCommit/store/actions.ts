import { UnavailableEntityApiErrorType } from 'core/services/shared/UnavailableEntityApiError';
import { AppError } from 'core/shared/models/Error';
import { IRepository } from 'core/shared/models/Repository/Repository';
import {
  ICommit,
  IHydratedCommit,
} from 'core/shared/models/Repository/RepositoryData';
import * as Actions from 'utils/redux/actions';

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
