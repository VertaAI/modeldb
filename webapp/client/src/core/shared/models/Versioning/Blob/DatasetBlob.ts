import { SHA } from '../RepositoryData';
import { GenericDiff } from './Diff';

export type IDatasetBlob = IPathDatasetBlob | IS3DatasetBlob;
export type IDatasetBlobDiff = IPathDatasetBlobDiff | IS3DatasetBlobDiff;

export interface IPathDatasetBlob {
  type: 'path';
  category: 'dataset';
  components: IPathDatasetComponentBlob[];
}
export type IPathDatasetBlobDiff = GenericDiff<
  IPathDatasetBlob,
  IPathDatasetBlob['category'],
  IPathDatasetBlob['type']
>;

export interface IS3DatasetBlob {
  category: 'dataset';
  type: 's3';
  components: IS3DatasetComponentBlob[];
}
export type IS3DatasetBlobDiff = GenericDiff<
  IS3DatasetBlob,
  IS3DatasetBlob['category'],
  IS3DatasetBlob['type']
>;
export interface IS3DatasetComponentBlob {
  path: IPathDatasetComponentBlob;
}

export interface IPathDatasetComponentBlob {
  path: string; // Full path to the file
  size: number;
  lastModifiedAtSource: Date;
  sha256: SHA;
  md5: string;
}

export const datasetBlobTypes: Record<
  IDatasetBlob['type'],
  IDatasetBlob['type']
> = {
  s3: 's3',
  path: 'path',
};
