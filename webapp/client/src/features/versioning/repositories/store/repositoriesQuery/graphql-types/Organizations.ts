/* tslint:disable */
/* eslint-disable */
// @generated
// This file was automatically generated and should not be edited.

// ====================================================
// GraphQL query operation: Organizations
// ====================================================

export interface Organizations_workspace_repositories_repositories_owner {
  id: string;
  name: string | null;
  email: string;
  picture: string | null;
}

export interface Organizations_workspace_repositories_repositories {
  name: string;
  id: string;
  dateCreated: any;
  dateUpdated: any;
  labels: string[];
  owner: Organizations_workspace_repositories_repositories_owner;
}

export interface Organizations_workspace_repositories {
  repositories: Organizations_workspace_repositories_repositories[];
}

export interface Organizations_workspace {
  repositories: Organizations_workspace_repositories;
}

export interface Organizations {
  workspace: Organizations_workspace | null;
}
