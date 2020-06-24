/* tslint:disable */
/* eslint-disable */
// @generated
// This file was automatically generated and should not be edited.

import { Visibility } from '../../../../../../graphql-types/graphql-global-types';

// ====================================================
// GraphQL mutation operation: CreateRepository
// ====================================================

export interface CreateRepository_workspace_createRepository {
  __typename: 'Repository';
  id: string;
}

export interface CreateRepository_workspace {
  __typename: 'Workspace';
  id: string;
  createRepository: CreateRepository_workspace_createRepository;
}

export interface CreateRepository {
  workspace: CreateRepository_workspace | null;
}

export interface CreateRepositoryVariables {
  workspaceName: string;
  name: string;
  visibility: Visibility;
}
