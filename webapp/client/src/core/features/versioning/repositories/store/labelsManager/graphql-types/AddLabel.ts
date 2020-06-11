/* tslint:disable */
/* eslint-disable */
// @generated
// This file was automatically generated and should not be edited.

// ====================================================
// GraphQL mutation operation: AddLabel
// ====================================================

export interface AddLabel_repository_addLabels {
  __typename: "Repository";
  id: string;
  labels: string[];
}

export interface AddLabel_repository {
  __typename: "Repository";
  id: string;
  addLabels: AddLabel_repository_addLabels;
}

export interface AddLabel {
  repository: AddLabel_repository | null;
}

export interface AddLabelVariables {
  id: string;
  label: string;
}
