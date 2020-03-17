import { AppError } from 'core/shared/models/Error';
import { IRepository } from 'core/shared/models/Repository/Repository';
import { SHA } from 'core/shared/models/Repository/RepositoryData';
import * as Actions from 'utils/redux/actions';

export const loadCommitsDiff = Actions.makeThunkApiRequest(
  '@@compareCommits/LOAD_COMMITS_DIFF_REQUEST',
  '@@compareCommits/LOAD_COMMITS_DIFF_SUCCESS',
  '@@compareCommits/LOAD_COMMITS_DIFF_FAILURE',
  '@@compareCommits/LOAD_COMMITS_DIFF_RESET'
)<
  {
    repositoryId: IRepository['id'];
    commitASha: SHA;
    commitBSha: SHA;
  },
  any,
  AppError,
  undefined
>(
  async ({ payload, dependencies: { ServiceFactory } }) =>
    await ServiceFactory.getCompareCommitsService().loadCommitsDiff(payload)
);
