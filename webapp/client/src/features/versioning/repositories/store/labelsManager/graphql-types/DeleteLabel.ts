/* tslint:disable */
/* eslint-disable */
// @generated
// This file was automatically generated and should not be edited.

// ====================================================
// GraphQL mutation operation: DeleteLabel
// ====================================================

export interface DeleteLabel_repository_deleteLabels {
  __typename: "Repository";
  id: string;
  labels: string[];
}

export interface DeleteLabel_repository {
  __typename: "Repository";
  id: string;
  deleteLabels: DeleteLabel_repository_deleteLabels;
}

export interface DeleteLabel {
  repository: DeleteLabel_repository | null;
}

export interface DeleteLabelVariables {
  id: string;
  label: string;
}
