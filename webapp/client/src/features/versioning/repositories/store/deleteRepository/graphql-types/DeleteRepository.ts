/* tslint:disable */
/* eslint-disable */
// @generated
// This file was automatically generated and should not be edited.

// ====================================================
// GraphQL mutation operation: DeleteRepository
// ====================================================

export interface DeleteRepository_repository {
  __typename: "Repository";
  id: string;
  delete: boolean;
}

export interface DeleteRepository {
  repository: DeleteRepository_repository | null;
}

export interface DeleteRepositoryVariables {
  id: string;
}
