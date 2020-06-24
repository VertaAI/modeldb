import { IDatasetVersion } from 'shared/models/DatasetVersion';
import ModelRecord from 'shared/models/ModelRecord';

export interface ICompareEntitiesState {
  data: {
    comparedEntityIdsByContainerId: Record<
      string,
      ComparedEntityIds | undefined
    >;
  };
}

export type ComparedEntityIds = [string?, string?];

// models

export type ComparedModels = [ModelRecord?, ModelRecord?];

export enum ComparedArtifactPropType {
  key = 'key',
  type = 'type',
  path = 'path',
  linkedArtifactId = 'linkedArtifactId',
}

export type CodeVersionDiffInfo =
  | { type: 'diffType' }
  | { type: 'artifactCodeVersion'; diffInfoByKeys: Record<string, boolean> }
  | { type: 'gitCodeVersion'; diffInfoByKeys: Record<string, boolean> };

export interface IModelsDifferentProps {
  id: boolean;
  ownerId: boolean;
  projectId: boolean;
  codeVersion: {
    usual: CodeVersionDiffInfo;
    fromBlobs: null | Record<string, CodeVersionDiffInfo>;
  };
  hyperparameters: {
    [key: string]: boolean;
  };
  metrics: {
    [key: string]: boolean;
  };
  experimentId: boolean;
  artifacts: {
    [key: string]: boolean;
  };
  attributes: IAttributesDiffInfo;
  datasets: {
    [key: string]: boolean;
  };
  tags: boolean;
}

export type IModelRecordView = Omit<ModelRecord, 'codeVersion'> & {
  codeVersion: {
    usual: ModelRecord['codeVersion'];
    fromBlobs: ModelRecord['codeVersionsFromBlob'];
  };
};

export type IAttributesDiffInfo = Record<
  string,
  | { type: 'singleAttribute' }
  | { type: 'singleValueTypes'; isDifferent: boolean }
  | { type: 'differentValueTypes' }
  | { type: 'listValueTypes'; diffInfo: { [value: string]: boolean } }
>;

// dataset versions

export type ComparedDatasetVersions = [IDatasetVersion?, IDatasetVersion?];

export interface IDatasetVersionCommonDifferentProps {
  id: boolean;
  parentId: boolean;
  ownerId: boolean;
  attributes: IAttributesDiffInfo;
  version: boolean;
  tags: boolean;
  dateLogged: boolean;
}
export type IRawDatasetVersionDifferentProps = {
  size: boolean;
  features: {
    [key: string]: boolean;
  };
  numRecords: boolean;
  objectPath: boolean;
  checkSum: boolean;
} & IDatasetVersionCommonDifferentProps;

export type IQueryDatasetVersionDifferentProps = {
  query: boolean;
  queryTemplate: boolean;
  queryParameters: {
    [key: string]: boolean;
  };
  dataSourceUri: boolean;
  executionTimestamp: boolean;
  numRecords: boolean;
} & IDatasetVersionCommonDifferentProps;

export type IPathDatasetVersionDifferentProps = {
  size: boolean;
  basePath: boolean;
  locationType: boolean;
  datasetPathInfos: IDiffDatasetPathInfos;
} & IDatasetVersionCommonDifferentProps;
export type IDiffDatasetPathInfos = Record<
  string,
  { path: boolean; size: boolean; checkSum: boolean; lastModified: boolean }
>;
export type IDatasetVersionsDifferentProps =
  | IRawDatasetVersionDifferentProps
  | IQueryDatasetVersionDifferentProps
  | IPathDatasetVersionDifferentProps;

//

export enum EntityType {
  entity1 = 'entity1',
  entity2 = 'entity2',
}

export enum selectEntityForComparingActionType {
  SELECT_ENTITY_FOR_COMPARING = '@@compareEntities/SELECT_ENTITY_FOR_COMPARING',
}
export interface ISelectEntityForComparing {
  type: selectEntityForComparingActionType.SELECT_ENTITY_FOR_COMPARING;
  payload: { projectId: string; modelRecordId: ModelRecord['id'] };
}
export enum unselectEntityForComparingActionType {
  UNSELECT_ENTITY_FOR_COMPARING = '@@compareEntities/UNSELECT_ENTITY_FOR_COMPARING',
}
export interface IUnselectEntityForComparing {
  type: unselectEntityForComparingActionType.UNSELECT_ENTITY_FOR_COMPARING;
  payload: { projectId: string; modelRecordId: ModelRecord['id'] };
}

export type FeatureAction =
  | ISelectEntityForComparing
  | IUnselectEntityForComparing;
