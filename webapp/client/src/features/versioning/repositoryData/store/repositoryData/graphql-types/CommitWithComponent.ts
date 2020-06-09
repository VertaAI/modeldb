/* tslint:disable */
/* eslint-disable */
// @generated
// This file was automatically generated and should not be edited.

import { CommitReference } from '../../../../../../graphql-types/graphql-global-types';

// ====================================================
// GraphQL query operation: CommitWithComponent
// ====================================================

export interface CommitWithComponent_repository_commitByReference_author {
  __typename: 'User';
  id: string;
  email: string;
  picture: string | null;
  username: string;
}

export interface CommitWithComponent_repository_commitByReference_getLocation_CommitFolder_subfolders {
  __typename: 'NamedCommitFolder';
  name: string;
}

export interface CommitWithComponent_repository_commitByReference_getLocation_CommitFolder_blobs {
  __typename: 'NamedCommitBlob';
  name: string;
}

export interface CommitWithComponent_repository_commitByReference_getLocation_CommitFolder {
  __typename: 'CommitFolder';
  subfolders: CommitWithComponent_repository_commitByReference_getLocation_CommitFolder_subfolders[];
  blobs: CommitWithComponent_repository_commitByReference_getLocation_CommitFolder_blobs[];
}

export interface CommitWithComponent_repository_commitByReference_getLocation_CommitBlob_runs_runs_experiment {
  __typename: 'Experiment';
  id: string;
  name: string;
}

export interface CommitWithComponent_repository_commitByReference_getLocation_CommitBlob_runs_runs_project {
  __typename: 'Project';
  id: string;
  name: string;
}

export interface CommitWithComponent_repository_commitByReference_getLocation_CommitBlob_runs_runs {
  __typename: 'ExperimentRun';
  id: string;
  name: string;
  experiment: CommitWithComponent_repository_commitByReference_getLocation_CommitBlob_runs_runs_experiment;
  project: CommitWithComponent_repository_commitByReference_getLocation_CommitBlob_runs_runs_project;
}

export interface CommitWithComponent_repository_commitByReference_getLocation_CommitBlob_runs {
  __typename: 'ExperimentRuns';
  runs: CommitWithComponent_repository_commitByReference_getLocation_CommitBlob_runs_runs[];
}

export interface CommitWithComponent_repository_commitByReference_getLocation_CommitBlob {
  __typename: 'CommitBlob';
  content: string;
  runs: CommitWithComponent_repository_commitByReference_getLocation_CommitBlob_runs;
}

export type CommitWithComponent_repository_commitByReference_getLocation =
  | CommitWithComponent_repository_commitByReference_getLocation_CommitFolder
  | CommitWithComponent_repository_commitByReference_getLocation_CommitBlob;

export interface CommitWithComponent_repository_commitByReference {
  __typename: 'Commit';
  id: string;
  message: string;
  date: GraphQLDate;
  author: CommitWithComponent_repository_commitByReference_author;
  getLocation: CommitWithComponent_repository_commitByReference_getLocation | null;
}

export interface CommitWithComponent_repository {
  __typename: 'Repository';
  id: string;
  commitByReference: CommitWithComponent_repository_commitByReference | null;
}

export interface CommitWithComponent {
  repository: CommitWithComponent_repository | null;
}

export interface CommitWithComponentVariables {
  repositoryId: string;
  location: string[];
  commitReference: CommitReference;
}
