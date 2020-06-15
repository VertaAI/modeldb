/* tslint:disable */
/* eslint-disable */
// @generated
// This file was automatically generated and should not be edited.

// ====================================================
// GraphQL mutation operation: MergeCommits
// ====================================================

export interface MergeCommits_repository_merge_commit {
  __typename: "Commit";
  id: string;
}

export interface MergeCommits_repository_merge {
  __typename: "MergeResult";
  commit: MergeCommits_repository_merge_commit | null;
  conflicts: string[] | null;
}

export interface MergeCommits_repository {
  __typename: "Repository";
  id: string;
  merge: MergeCommits_repository_merge;
}

export interface MergeCommits {
  repository: MergeCommits_repository | null;
}

export interface MergeCommitsVariables {
  repositoryId: string;
  commitASha: string;
  commitBSha: string;
}
