/* tslint:disable */
/* eslint-disable */
// @generated
// This file was automatically generated and should not be edited.

// ====================================================
// GraphQL query operation: COMMIT_DETAILS
// ====================================================

export interface COMMIT_DETAILS_repository_commit_author {
  __typename: "User";
  id: string;
  email: string;
  picture: string | null;
  username: string;
}

export interface COMMIT_DETAILS_repository_commit_asDiff {
  __typename: "CommitAsDiff";
  parent: string;
  diff: string[] | null;
}

export interface COMMIT_DETAILS_repository_commit_runs_runs_experiment {
  __typename: "Experiment";
  id: string;
  name: string;
}

export interface COMMIT_DETAILS_repository_commit_runs_runs_project {
  __typename: "Project";
  id: string;
  name: string;
}

export interface COMMIT_DETAILS_repository_commit_runs_runs {
  __typename: "ExperimentRun";
  id: string;
  name: string;
  experiment: COMMIT_DETAILS_repository_commit_runs_runs_experiment;
  project: COMMIT_DETAILS_repository_commit_runs_runs_project;
}

export interface COMMIT_DETAILS_repository_commit_runs {
  __typename: "ExperimentRuns";
  runs: COMMIT_DETAILS_repository_commit_runs_runs[];
}

export interface COMMIT_DETAILS_repository_commit {
  __typename: "Commit";
  id: string;
  date: GraphQLDate;
  message: string;
  author: COMMIT_DETAILS_repository_commit_author;
  asDiff: COMMIT_DETAILS_repository_commit_asDiff | null;
  runs: COMMIT_DETAILS_repository_commit_runs;
}

export interface COMMIT_DETAILS_repository {
  __typename: "Repository";
  id: string;
  commit: COMMIT_DETAILS_repository_commit | null;
}

export interface COMMIT_DETAILS {
  repository: COMMIT_DETAILS_repository | null;
}

export interface COMMIT_DETAILSVariables {
  repositoryId: string;
  commitSha: string;
}
