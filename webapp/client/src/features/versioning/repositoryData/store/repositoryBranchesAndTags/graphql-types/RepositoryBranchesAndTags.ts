/* tslint:disable */
/* eslint-disable */
// @generated
// This file was automatically generated and should not be edited.

// ====================================================
// GraphQL query operation: RepositoryBranchesAndTags
// ====================================================

export interface RepositoryBranchesAndTags_repository_tags {
  __typename: "RepositoryTag";
  name: string;
}

export interface RepositoryBranchesAndTags_repository_branches {
  __typename: "RepositoryBranch";
  name: string;
}

export interface RepositoryBranchesAndTags_repository {
  __typename: "Repository";
  id: string;
  tags: RepositoryBranchesAndTags_repository_tags[];
  branches: RepositoryBranchesAndTags_repository_branches[];
}

export interface RepositoryBranchesAndTags {
  repository: RepositoryBranchesAndTags_repository | null;
}

export interface RepositoryBranchesAndTagsVariables {
  repositoryId: string;
}
