import { AppError } from 'core/shared/models/Error';
import {
  DataWithPagination,
  IPaginationSettings,
} from 'core/shared/models/Pagination';
import { CommitComponentLocation } from 'core/shared/models/Versioning/CommitComponentLocation';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import {
  IHydratedCommit,
  Branch,
} from 'core/shared/models/Versioning/RepositoryData';
import * as Actions from 'utils/redux/actions';

export const loadCommits = Actions.makeThunkApiRequest(
  '@@commitsHistory/LOAD_COMMITS_REQUEST',
  '@@commitsHistory/LOAD_COMMITS_SUCCESS',
  '@@commitsHistory/LOAD_COMMITS_FAILURE',
  '@@commitsHistory/LOAD_COMMITS_RESET'
)<
  {
    repositoryId: IRepository['id'];
    branch: Branch;
    paginationSettings: IPaginationSettings;
    location: CommitComponentLocation;
  },
  DataWithPagination<IHydratedCommit>,
  AppError,
  undefined
>(async ({ dependencies: { ServiceFactory }, payload }) => {
  return await ServiceFactory.getRepositoryDataService().loadBranchCommits(
    payload
  );
});
