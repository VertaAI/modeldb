/* tslint:disable */
/* eslint-disable */
// @generated
// This file was automatically generated and should not be edited.

import { AccessType } from '../../../../../../graphql-types/graphql-global-types';

// ====================================================
// GraphQL query operation: Repository
// ====================================================

export interface Repository_workspace_repository_owner {
  __typename: 'User';
  id: string;
  email: string;
  picture: string | null;
  username: string;
}

export interface Repository_workspace_repository_collaborators_UserCollaborator_user {
  __typename: 'User';
  id: string;
  email: string;
  picture: string | null;
  username: string;
}

export interface Repository_workspace_repository_collaborators_UserCollaborator {
  __typename: 'UserCollaborator';
  user: Repository_workspace_repository_collaborators_UserCollaborator_user;
  type: AccessType;
  canDeploy: boolean;
}

export interface Repository_workspace_repository_collaborators_TeamCollaborator_team {
  __typename: 'Team';
  id: string;
  name: string;
}

export interface Repository_workspace_repository_collaborators_TeamCollaborator {
  __typename: 'TeamCollaborator';
  team: Repository_workspace_repository_collaborators_TeamCollaborator_team;
  type: AccessType;
  canDeploy: boolean;
}

export type Repository_workspace_repository_collaborators =
  | Repository_workspace_repository_collaborators_UserCollaborator
  | Repository_workspace_repository_collaborators_TeamCollaborator;

export interface Repository_workspace_repository_allowedActions {
  __typename: 'AllowedActions';
  create: boolean;
  update: boolean;
  delete: boolean;
}

export interface Repository_workspace_repository {
  __typename: 'Repository';
  name: string;
  id: string;
  dateCreated: GraphQLDate;
  dateUpdated: GraphQLDate;
  labels: string[];
  owner: Repository_workspace_repository_owner;
  collaborators: Repository_workspace_repository_collaborators[];
  allowedActions: Repository_workspace_repository_allowedActions;
}

export interface Repository_workspace {
  __typename: 'Workspace';
  id: string;
  repository: Repository_workspace_repository | null;
}

export interface Repository {
  workspace: Repository_workspace | null;
}

export interface RepositoryVariables {
  workspaceName: string;
  repositoryName: string;
}
