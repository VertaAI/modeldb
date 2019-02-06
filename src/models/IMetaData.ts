import ModelRecord from './ModelRecord';
import Project from './Project';

export interface IMetaData {
  propertyName?: string;
}

export type MetaData = Project | ModelRecord;
