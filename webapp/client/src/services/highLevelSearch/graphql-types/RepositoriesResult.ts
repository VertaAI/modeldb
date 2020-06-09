/* tslint:disable */
/* eslint-disable */
// @generated
// This file was automatically generated and should not be edited.

import {
  StringPredicate,
  PaginationQuery,
} from '../../../graphql-types/graphql-global-types';

// ====================================================
// GraphQL query operation: RepositoriesResult
// ====================================================

export interface RepositoriesResult_workspace_repositories_repositories_owner {
  __typename: 'User';
  username: string;
}

export interface RepositoriesResult_workspace_repositories_repositories {
  __typename: 'Repository';
  id: string;
  name: string;
  owner: RepositoriesResult_workspace_repositories_repositories_owner;
  dateCreated: GraphQLDate;
  dateUpdated: GraphQLDate;
  labels: string[];
}

export interface RepositoriesResult_workspace_repositories_pagination {
  __typename: 'PaginationResponse';
  totalRecords: number;
}

export interface RepositoriesResult_workspace_repositories {
  __typename: 'Repositories';
  repositories: RepositoriesResult_workspace_repositories_repositories[];
  pagination: RepositoriesResult_workspace_repositories_pagination;
}

export interface RepositoriesResult_workspace {
  __typename: 'Workspace';
  name: string;
  repositories: RepositoriesResult_workspace_repositories;
}

export interface RepositoriesResult {
  workspace: RepositoriesResult_workspace | null;
}

export interface RepositoriesResultVariables {
  workspaceName: string;
  filters: StringPredicate[];
  pagination: PaginationQuery;
}
