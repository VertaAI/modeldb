import { IAttribute } from 'core/shared/models/Attribute';
import { DatasetType } from './Dataset';

interface ICommonDatasetVersion<Type extends DatasetType, Info> {
  id: string;
  parentId?: string;
  datasetId: string;
  dateLogged: Date;
  dateUpdated: Date;
  description: string;
  tags: string[];
  isPubliclyVisible: boolean;
  attributes: IAttribute[];
  version: number;
  type: Type;
  info: Info;
}

export type IQueryDatasetVersion = ICommonDatasetVersion<
  'query',
  IQueryDatasetVersionInfo
>;
export interface IQueryDatasetVersionInfo {
  query?: string;
  queryTemplate?: string;
  queryParameters: Array<{ name: string; value: string }>;
  dataSourceUri?: string;
  executionTimestamp?: number;
  numRecords?: number;
}

export type IRawDatasetVersion = ICommonDatasetVersion<
  'raw',
  IRawDatasetVersionInfo
>;
export interface IRawDatasetVersionInfo {
  size?: number;
  features: string[];
  numRecords?: number;
  objectPath?: string;
  checkSum?: string;
}

export type IPathBasedDatasetVersion = ICommonDatasetVersion<
  'path',
  IPathBasedDatasetVersionInfo
>;
export interface IPathBasedDatasetVersionInfo {
  size?: number;
  datasetPathInfos: IDatasetPathPartInfo[];
  basePath?: string;
  locationType?: DatasetVersionPathLocationType;
}
export interface IDatasetPathPartInfo {
  path: string;
  size: number | string;
  checkSum: string;
  lastModified: Date;
}
export const DatasetVersionPathLocationTypes = {
  localFileSystem: 'localFileSystem',
  networkFileSystem: 'networkFileSystem',
  hadoopFileSystem: 'hadoopFileSystem',
  s3FileSystem: 's3FileSystem',
} as const;
export type DatasetVersionPathLocationType = (typeof DatasetVersionPathLocationTypes)[keyof (typeof DatasetVersionPathLocationTypes)];

export type IDatasetVersion =
  | IQueryDatasetVersion
  | IRawDatasetVersion
  | IPathBasedDatasetVersion;
