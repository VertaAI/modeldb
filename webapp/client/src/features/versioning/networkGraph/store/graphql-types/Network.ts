/* tslint:disable */
/* eslint-disable */
// @generated
// This file was automatically generated and should not be edited.

import { NetworkEdgeType } from '../../../../../graphql-types/graphql-global-types';

// ====================================================
// GraphQL query operation: Network
// ====================================================

export interface Network_workspace_repository_network_commits_commit_author {
  __typename: 'User';
  picture: string | null;
}

export interface Network_workspace_repository_network_commits_commit {
  __typename: 'Commit';
  message: string;
  author: Network_workspace_repository_network_commits_commit_author;
  sha: string;
}

export interface Network_workspace_repository_network_commits {
  __typename: 'NetworkCommitColor';
  commit: Network_workspace_repository_network_commits_commit;
  color: number;
}

export interface Network_workspace_repository_network_branches {
  __typename: 'NetworkBranchColor';
  branch: string;
  color: number;
  commitIndex: number;
}

export interface Network_workspace_repository_network_edges {
  __typename: 'NetworkEdgeColor';
  fromCommitIndex: number;
  toCommitIndex: number;
  color: number;
  edgeType: NetworkEdgeType;
}

export interface Network_workspace_repository_network {
  __typename: 'BranchesNetwork';
  commits: Network_workspace_repository_network_commits[];
  branches: Network_workspace_repository_network_branches[];
  edges: Network_workspace_repository_network_edges[];
}

export interface Network_workspace_repository {
  __typename: 'Repository';
  id: string;
  network: Network_workspace_repository_network;
}

export interface Network_workspace {
  __typename: 'Workspace';
  id: string;
  repository: Network_workspace_repository | null;
}

export interface Network {
  workspace: Network_workspace | null;
}

export interface NetworkVariables {
  workspaceName: string;
  repositoryName: string;
}
