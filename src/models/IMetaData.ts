import { Model } from './Model';
import Project from './Project';

export interface IMetaData {
  propertyName?: string;
}

export type MetaData = Project | Model;
