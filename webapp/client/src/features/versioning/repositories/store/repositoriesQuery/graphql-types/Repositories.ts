/* tslint:disable */
/* eslint-disable */
// @generated
// This file was automatically generated and should not be edited.

import {
  PaginationQuery,
  AccessType,
} from '../../../../../../graphql-types/graphql-global-types';

// ====================================================
// GraphQL query operation: Repositories
// ====================================================

export interface Repositories_workspace_repositories_repositories_owner {
  __typename: 'User';
  id: string;
  email: string;
  picture: string | null;
  username: string;
}

export interface Repositories_workspace_repositories_repositories_collaborators_UserCollaborator_user {
  __typename: 'User';
  id: string;
  email: string;
  picture: string | null;
  username: string;
}

export interface Repositories_workspace_repositories_repositories_collaborators_UserCollaborator {
  __typename: 'UserCollaborator';
  user: Repositories_workspace_repositories_repositories_collaborators_UserCollaborator_user;
  type: AccessType;
  canDeploy: boolean;
}

export interface Repositories_workspace_repositories_repositories_collaborators_TeamCollaborator_team {
  __typename: 'Team';
  id: string;
  name: string;
}

export interface Repositories_workspace_repositories_repositories_collaborators_TeamCollaborator {
  __typename: 'TeamCollaborator';
  team: Repositories_workspace_repositories_repositories_collaborators_TeamCollaborator_team;
  type: AccessType;
  canDeploy: boolean;
}

export type Repositories_workspace_repositories_repositories_collaborators =
  | Repositories_workspace_repositories_repositories_collaborators_UserCollaborator
  | Repositories_workspace_repositories_repositories_collaborators_TeamCollaborator;

export interface Repositories_workspace_repositories_repositories_allowedActions {
  __typename: 'AllowedActions';
  create: boolean;
  update: boolean;
  delete: boolean;
}

export interface Repositories_workspace_repositories_repositories {
  __typename: 'Repository';
  name: string;
  id: string;
  dateCreated: GraphQLDate;
  dateUpdated: GraphQLDate;
  labels: string[];
  owner: Repositories_workspace_repositories_repositories_owner;
  collaborators: Repositories_workspace_repositories_repositories_collaborators[];
  allowedActions: Repositories_workspace_repositories_repositories_allowedActions;
}

export interface Repositories_workspace_repositories_pagination {
  __typename: 'PaginationResponse';
  totalRecords: number;
}

export interface Repositories_workspace_repositories {
  __typename: 'Repositories';
  repositories: Repositories_workspace_repositories_repositories[];
  pagination: Repositories_workspace_repositories_pagination;
}

export interface Repositories_workspace {
  __typename: 'Workspace';
  id: string;
  repositories: Repositories_workspace_repositories;
}

export interface Repositories {
  workspace: Repositories_workspace | null;
}

export interface RepositoriesVariables {
  workspaceName: string;
  pagination: PaginationQuery;
}
