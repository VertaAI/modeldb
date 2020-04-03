import { UnavailableEntityApiErrorType } from 'core/services/shared/UnavailableEntityApiError';
import { AppError } from 'core/shared/models/Error';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import * as Actions from 'utils/redux/actions';

import {
  ICommit,
  CommitPointer,
} from 'core/shared/models/Versioning/RepositoryData';
import * as DataLocation from 'core/shared/models/Versioning/DataLocation';

import { ICommitPointersCommits, IComparedCommitPointersInfo } from './types';
import * as routeHelpers from '../../repositoryData/view/RepositoryData/routeHelpers';

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

export const mergeCommits = Actions.makeThunkApiRequest(
  '@@compareChanges/LOAD_MERGE_COMMITS_REQUEST',
  '@@compareChanges/LOAD_MERGE_COMMITS_SUCCESS',
  '@@compareChanges/LOAD_MERGE_COMMITS_FAILURE',
  '@@compareChanges/LOAD_MERGE_COMMITS_RESET'
)<
  {
    repositoryId: IRepository['id'];
    repositoryName: IRepository['name'];
    base: CommitPointer;
    commitASha: ICommit['sha'];
    commitBSha: ICommit['sha'];
  },
  ICommit,
  AppError<UnavailableEntityApiErrorType>
>(
  async ({ payload, dependencies: { ServiceFactory } }) => {
    return await ServiceFactory.getCompareCommitsService().mergeCommits(
      payload
    );
  },
  {
    onSuccess: async ({ requestPayload, dependencies }) => {
      dependencies.history.push(
        routeHelpers.getRedirectPath({
          commitPointer: requestPayload.base,
          location: DataLocation.makeRoot(),
          repositoryName: requestPayload.repositoryName,
          type: 'folder',
        })
      );
    },
  }
);
