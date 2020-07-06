/* tslint:disable */
/* eslint-disable */
// @generated
// This file was automatically generated and should not be edited.

//==============================================================
// START Enums and Input Objects
//==============================================================

export enum AccessType {
  OWNER = "OWNER",
  READ_ONLY = "READ_ONLY",
  READ_WRITE = "READ_WRITE",
}

export enum BuildStatus {
  BUILDING = "BUILDING",
  DELETING = "DELETING",
  ERROR = "ERROR",
  FINISHED = "FINISHED",
}

export enum EndpointEnvironmentStatus {
  ACTIVE = "ACTIVE",
  CREATING = "CREATING",
  ERROR = "ERROR",
  INACTIVE = "INACTIVE",
  UNKNOWN = "UNKNOWN",
  UPDATING = "UPDATING",
}

export enum NetworkEdgeType {
  BRANCH = "BRANCH",
  DEFAULT = "DEFAULT",
  MERGE = "MERGE",
}

export enum PredicateOperator {
  CONTAIN = "CONTAIN",
  EQ = "EQ",
  GT = "GT",
  GTE = "GTE",
  IN = "IN",
  LT = "LT",
  LTE = "LTE",
  NE = "NE",
  NOT_CONTAIN = "NOT_CONTAIN",
}

export enum Visibility {
  ORG_SCOPED_PUBLIC = "ORG_SCOPED_PUBLIC",
  PRIVATE = "PRIVATE",
  PUBLIC = "PUBLIC",
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

export interface StringPredicate {
  key: string;
  value: string;
  operator: PredicateOperator;
}

//==============================================================
// END Enums and Input Objects
//==============================================================
