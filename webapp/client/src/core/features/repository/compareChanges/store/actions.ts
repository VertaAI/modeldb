import { UnavailableEntityApiErrorType } from 'core/services/shared/UnavailableEntityApiError';
import { AppError } from 'core/shared/models/Error';
import * as DataLocation from 'core/shared/models/Repository/DataLocation';
import { IRepository } from 'core/shared/models/Repository/Repository';
import * as Actions from 'utils/redux/actions';

import { ICommitPointersCommits, IComparedCommitPointersInfo } from './types';

export const loadCommitPointersCommits = Actions.makeThunkApiRequest(
  '@@compareChanges/LOAD_COMMIT_POINTERS_COMMITS_REQUEST',
  '@@compareChanges/LOAD_COMMIT_POINTERS_COMMITS_SUCCESS',
  '@@compareChanges/LOAD_COMMIT_POINTERS_COMMITS_FAILURE',
  '@@compareChanges/LOAD_COMMIT_POINTERS_COMMITS_RESET'
)<
  {
    repositoryId: IRepository['id'];
    comparedCommitPointersInfo: IComparedCommitPointersInfo;
  },
  ICommitPointersCommits,
  AppError<UnavailableEntityApiErrorType>
>(async ({ payload, dependencies: { ServiceFactory } }) => {
  const [commitPointerACommit, commitPointerBCommit] = await Promise.all([
    ServiceFactory.getRepositoryDataService().loadCommitByPointer(
      payload.repositoryId,
      payload.comparedCommitPointersInfo.commitPointerA
    ),
    ServiceFactory.getRepositoryDataService().loadCommitByPointer(
      payload.repositoryId,
      payload.comparedCommitPointersInfo.commitPointerB
    ),
  ]);

  return {
    commitPointerA: commitPointerACommit,
    commitPointerB: commitPointerBCommit,
  };
});
