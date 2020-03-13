import { IAttribute } from 'core/shared/models/Attribute';
import { IEntityWithLogging } from 'core/shared/models/Common';

import { IWorkspace, IEntityWithShortWorkspace } from './Workspace';

export type IDataset = Dataset;

export interface Dataset extends IEntityWithLogging, IEntityWithShortWorkspace {
  id: string;
  name: string;
  description: string;
  tags: string[];
  type: DatasetType;
  isPubliclyVisible: boolean;
  attributes: IAttribute[];
}

export interface IDatasetCreationSettings {
  name: string;
  visibility: DatasetVisibility;
  tags?: string[];
  description?: string;
  type: DatasetType;
}

export const DatasetVisibility = {
  private: 'private',
  // to be enabled after backend updtae
  // public: 'public',
};
export type DatasetVisibility = keyof typeof DatasetVisibility;

export interface ShortDataset {
  id: string;
  name: string;
}

export const DatasetType = {
  path: 'path',
  query: 'query',
  raw: 'raw',
};
export type DatasetType = keyof (typeof DatasetType);
