/* tslint:disable */
/* eslint-disable */
// @generated
// This file was automatically generated and should not be edited.

import { CommitReference } from '../../../../../../graphql-types/graphql-global-types';

// ====================================================
// GraphQL mutation operation: MergeConflicts
// ====================================================

export interface MergeConflicts_repository_commitA {
  __typename: 'Commit';
  id: string;
  sha: string;
}

export interface MergeConflicts_repository_commitB {
  __typename: 'Commit';
  id: string;
  sha: string;
}

export interface MergeConflicts_repository_merge_commonBase {
  __typename: 'Commit';
  id: string;
  sha: string;
}

export interface MergeConflicts_repository_merge {
  __typename: 'MergeResult';
  commonBase: MergeConflicts_repository_merge_commonBase | null;
  conflicts: string[] | null;
}

export interface MergeConflicts_repository {
  __typename: 'Repository';
  id: string;
  commitA: MergeConflicts_repository_commitA | null;
  commitB: MergeConflicts_repository_commitB | null;
  merge: MergeConflicts_repository_merge;
}

export interface MergeConflicts {
  repository: MergeConflicts_repository | null;
}

export interface MergeConflictsVariables {
  repositoryId: string;
  commitReferenceA: CommitReference;
  commitReferenceB: CommitReference;
  isDryRun: boolean;
}
