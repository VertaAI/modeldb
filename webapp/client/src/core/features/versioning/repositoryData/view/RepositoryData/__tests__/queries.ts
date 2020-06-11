import * as CommitComponentLocation from 'core/shared/models/Versioning/CommitComponentLocation';
import {
  ICommitWithComponent,
  CommitPointer,
} from 'core/shared/models/Versioning/RepositoryData';
import {
  IBranchesAndTags,
  IRepository,
} from 'core/shared/models/Versioning/Repository';
import makeGraphqlMockedResponse from 'core/shared/utils/tests/graphql/makeGraphqlMockedResponse';
import { getCommitReference } from 'core/shared/graphql/Versioning/CommitReference';
import { convertUserToServer } from 'core/shared/graphql/User/User';

import * as BranchesAndTagsQuery from '../../../store/repositoryBranchesAndTags/useRepositoryBranchesAndTags';
import * as CommitWithComponentQuery from '../../../store/repositoryData/useRepositoryData';

export const makeBranchesAndTagsQuery = (
  repositoryId: IRepository['id'],
  { branches, tags }: IBranchesAndTags
) => {
  return makeGraphqlMockedResponse<
    BranchesAndTagsQuery.Types.RepositoryBranchesAndTags,
    BranchesAndTagsQuery.Types.RepositoryBranchesAndTagsVariables
  >(BranchesAndTagsQuery.REPOSITORY_BRANCHES_AND_TAGS, {
    variables: { repositoryId },
    getResult: () => ({
      data: {
        repository: {
          __typename: 'Repository' as const,
          id: repositoryId,
          branches: branches.map(branch => ({
            __typename: 'RepositoryBranch',
            name: branch,
          })),
          tags: tags.map(tag => ({ __typename: 'RepositoryTag', name: tag })),
        },
      },
    }),
  });
};

export type MakeCommitWithComponentQuerySettings = {
  location: CommitComponentLocation.CommitComponentLocation;
  commitPointer: CommitPointer;
  commitWithComponent: ICommitWithComponent;
};
export const makeCommitWithComponentQuery = (
  repositoryId: IRepository['id'],
  {
    location,
    commitPointer,
    commitWithComponent: { commit, component },
  }: MakeCommitWithComponentQuerySettings
) => {
  return makeGraphqlMockedResponse<
    CommitWithComponentQuery.Types.CommitWithComponent,
    CommitWithComponentQuery.Types.CommitWithComponentVariables
  >(CommitWithComponentQuery.COMMIT_WITH_COMPONENT, {
    variables: {
      repositoryId,
      commitReference: getCommitReference(commitPointer),
      location,
    },
    getResult: () => {
      return {
        data: {
          repository: {
            __typename: 'Repository',
            id: repositoryId,
            commitByReference: {
              __typename: 'Commit',
              author: convertUserToServer(commit.author),
              date: String(commit.dateCreated.getMilliseconds()),
              id: commit.sha,
              message: commit.message,
              getLocation:
                component.type === 'folder'
                  ? {
                      __typename: 'CommitFolder',
                      blobs: component.blobs.map(({ name }) => ({
                        __typename: 'NamedCommitBlob',
                        name,
                      })),
                      subfolders: component.subFolders.map(({ name }) => ({
                        __typename: 'NamedCommitFolder',
                        name,
                      })),
                    }
                  : {
                      __typename: 'CommitBlob',
                      content: '{}',
                      runs: {
                        __typename: 'ExperimentRuns',
                        runs: [],
                      },
                    },
            },
          },
        },
      };
    },
  });
};
