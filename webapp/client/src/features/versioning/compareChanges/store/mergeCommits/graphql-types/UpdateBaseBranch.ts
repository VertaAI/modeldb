/* tslint:disable */
/* eslint-disable */
// @generated
// This file was automatically generated and should not be edited.

// ====================================================
// GraphQL mutation operation: UpdateBaseBranch
// ====================================================

export interface UpdateBaseBranch_repository_commit_setBranch {
  __typename: "Repository";
  id: string;
}

export interface UpdateBaseBranch_repository_commit {
  __typename: "Commit";
  setBranch: UpdateBaseBranch_repository_commit_setBranch;
}

export interface UpdateBaseBranch_repository {
  __typename: "Repository";
  id: string;
  commit: UpdateBaseBranch_repository_commit | null;
}

export interface UpdateBaseBranch {
  repository: UpdateBaseBranch_repository | null;
}

export interface UpdateBaseBranchVariables {
  repositoryId: string;
  commitSha: string;
  branch: string;
}
