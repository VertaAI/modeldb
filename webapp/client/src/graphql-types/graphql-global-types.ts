/* tslint:disable */
/* eslint-disable */
// @generated
// This file was automatically generated and should not be edited.

//==============================================================
// START Enums and Input Objects
//==============================================================

export enum AccessType {
  OWNER = 'OWNER',
  READ_ONLY = 'READ_ONLY',
  READ_WRITE = 'READ_WRITE',
}

export enum NetworkEdgeType {
  BRANCH = 'BRANCH',
  DEFAULT = 'DEFAULT',
  MERGE = 'MERGE',
}

export enum Visibility {
  ORG_SCOPED_PUBLIC = 'ORG_SCOPED_PUBLIC',
  PRIVATE = 'PRIVATE',
  PUBLIC = 'PUBLIC',
}

export interface CommitReference {
  commit?: string | null;
  tag?: string | null;
  branch?: string | null;
}

export interface PaginationQuery {
  page?: number | null;
  limit?: number | null;
}

//==============================================================
// END Enums and Input Objects
//==============================================================
