import { SHA } from 'core/shared/models/Versioning/RepositoryData';
import { getCommitReference } from 'core/shared/graphql/Versioning/CommitReference';
import makeGraphqlMockedResponse from 'core/shared/utils/tests/graphql/makeGraphqlMockedResponse';
import { IServerBlobDiff } from 'core/services/serverModel/Versioning/CompareCommits/ServerDiff';

import * as CompareChangesQuery from '../../../store/compareChanges/useCompareChanges';
import * as MergeCommits from '../../../store/mergeCommits/useMergeCommits';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import { ICommitPointerWithCommitSha } from './CompareChanges.test';

export const makeMergeCommitsMutationMock = (
  {
    commitASha,
    commitBSha,
    repositoryId,
  }: { repositoryId: IRepository['id']; commitASha: SHA; commitBSha: SHA },
  res: { type: 'conflict' } | { type: 'success'; newCommitSha: SHA }
) =>
  makeGraphqlMockedResponse<
    MergeCommits.MergeCommitsTypes.MergeCommits,
    MergeCommits.MergeCommitsTypes.MergeCommitsVariables
  >(MergeCommits.MERGE_COMMITS, {
    variables: { repositoryId, commitASha, commitBSha },
    getResult: () => ({
      data: {
        repository: {
          id: repositoryId,
          __typename: 'Repository' as const,
          merge:
            res.type === 'conflict'
              ? {
                  __typename: 'MergeResult' as const,
                  conflicts: [],
                  commit: null,
                }
              : {
                  __typename: 'MergeResult' as const,
                  conflicts: null,
                  commit: {
                    __typename: 'Commit' as const,
                    id: res.newCommitSha,
                  },
                },
        },
      },
    }),
  });
export const makeUpdateBranchMutationMock = ({
  repositoryId,
  baseBranch,
  newCommitSha,
}: {
  repositoryId: IRepository['id'];
  baseBranch: string;
  newCommitSha: SHA;
}) =>
  makeGraphqlMockedResponse<
    MergeCommits.UpdateBaseBranchTypes.UpdateBaseBranch,
    MergeCommits.UpdateBaseBranchTypes.UpdateBaseBranchVariables
  >(MergeCommits.UPDATE_BASE_BRANCH, {
    variables: {
      repositoryId: repositoryId,
      branch: baseBranch,
      commitSha: newCommitSha,
    },
    getResult: () => ({
      data: {
        repository: {
          __typename: 'Repository' as const,
          id: repositoryId,
          commit: {
            __typename: 'Commit',
            setBranch: {
              __typename: 'Repository',
              id: repositoryId,
            },
          },
        },
      },
    }),
  });

export type MakeCompareChangesQuerySettings = {
  repositoryId: IRepository['id'];
  A: ICommitPointerWithCommitSha;
  B: ICommitPointerWithCommitSha;
};
export const makeCompareChangesQuery = (
  { repositoryId, A, B }: MakeCompareChangesQuerySettings,
  diffs: IServerBlobDiff<any>[]
) =>
  makeGraphqlMockedResponse<
    CompareChangesQuery.Types.CompareChanges,
    CompareChangesQuery.Types.CompareChangesVariables
  >(CompareChangesQuery.COMPARE_CHANGES, {
    variables: {
      repositoryId: repositoryId,
      commitAReference: getCommitReference(A.pointer),
      commitBReference: getCommitReference(B.pointer),
    },
    getResult: () => ({
      data: {
        repository: {
          __typename: 'Repository',
          commitA: {
            __typename: 'Commit',
            id: A.commitSha,
          },
          commitB: {
            __typename: 'Commit',
            id: B.commitSha,
          },
          id: repositoryId,
          diff: diffs.map(diff => JSON.stringify(diff)),
        },
      },
    }),
  });
