/* tslint:disable */
/* eslint-disable */
// @generated
// This file was automatically generated and should not be edited.

import { CommitReference } from './../../../../../../../graphql-types/graphql-global-types';

// ====================================================
// GraphQL query operation: CompareChanges
// ====================================================

export interface CompareChanges_repository_commitA {
  __typename: 'Commit';
  id: string;
}

export interface CompareChanges_repository_commitB {
  __typename: 'Commit';
  id: string;
}

export interface CompareChanges_repository {
  __typename: 'Repository';
  id: string;
  commitA: CompareChanges_repository_commitA | null;
  commitB: CompareChanges_repository_commitB | null;
  diff: string[] | null;
}

export interface CompareChanges {
  repository: CompareChanges_repository | null;
}

export interface CompareChangesVariables {
  repositoryId: string;
  commitAReference: CommitReference;
  commitBReference: CommitReference;
}
