/* tslint:disable */
/* eslint-disable */
// @generated
// This file was automatically generated and should not be edited.

import { CommitReference } from '../../../../../../graphql-types/graphql-global-types';

// ====================================================
// GraphQL query operation: CommitsHistory
// ====================================================

export interface CommitsHistory_repository_log_commits_author {
  __typename: 'User';
  id: string;
  email: string;
  picture: string | null;
  username: string;
}

export interface CommitsHistory_repository_log_commits {
  __typename: 'Commit';
  id: string;
  message: string;
  date: GraphQLDate;
  author: CommitsHistory_repository_log_commits_author;
}

export interface CommitsHistory_repository_log {
  __typename: 'Commits';
  commits: CommitsHistory_repository_log_commits[];
}

export interface CommitsHistory_repository {
  __typename: 'Repository';
  id: string;
  log: CommitsHistory_repository_log;
}

export interface CommitsHistory {
  repository: CommitsHistory_repository | null;
}

export interface CommitsHistoryVariables {
  repositoryId: string;
  commitReference: CommitReference;
}
