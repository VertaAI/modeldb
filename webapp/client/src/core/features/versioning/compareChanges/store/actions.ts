import { UnavailableEntityApiErrorType } from 'core/services/shared/UnavailableEntityApiError';
import { AppError } from 'core/shared/models/Error';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import * as Actions from 'utils/redux/actions';

import * as CommitComponentLocation from 'core/shared/models/Versioning/CommitComponentLocation';
import {
  ICommit,
  CommitPointer,
  isMergeCommitsError,
} from 'core/shared/models/Versioning/RepositoryData';
import routes from 'routes';
import { selectCurrentWorkspaceName } from 'store/workspaces';

import {
  ICommitPointersCommits,
  IComparedCommitPointersInfo,
  MergeCommitCommunicationError,
} from './types';

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
  '@@compareChanges/MERGE_COMMITS_REQUEST',
  '@@compareChanges/MERGE_COMMITS_SUCCESS',
  '@@compareChanges/MERGE_COMMITS_FAILURE',
  '@@compareChanges/MERGE_COMMITS_RESET'
)<
  {
    repositoryId: IRepository['id'];
    repositoryName: IRepository['name'];
    base: CommitPointer;
    commitASha: ICommit['sha'];
    commitBSha: ICommit['sha'];
  },
  ICommit,
  MergeCommitCommunicationError
>(
  async ({ payload, dependencies: { ServiceFactory } }) => {
    return await ServiceFactory.getCompareCommitsService().mergeCommits(
      payload
    );
  },
  {
    onSuccess: async ({ requestPayload, dependencies, getState }) => {
      dependencies.history.push(
        routes.repositoryDataWithLocation.getRedirectPath({
          workspaceName: selectCurrentWorkspaceName(getState()),
          commitPointer: requestPayload.base,
          location: CommitComponentLocation.makeRoot(),
          repositoryName: requestPayload.repositoryName,
          type: 'folder',
        })
      );
    },
  },
  ({ error, rawError }) => {
    return isMergeCommitsError(rawError)
      ? { type: 'mergeCommitsError' }
      : { type: 'error', appError: error };
  }
);
