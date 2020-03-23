import { DataLocation } from '../DataLocation';
import { ICodeBlobDiff } from './CodeBlob';
import { IConfigBlobDiff } from './ConfigBlob';
import { IDatasetBlobDiff } from './DatasetBlob';
import { IEnvironmentBlobDiff } from './EnvironmentBlob';
import matchBy from 'core/shared/utils/matchBy';

export type ComparedCommitType = 'A' | 'B';
export type DiffType = Diff['diffType'];
export type Diff =
  | ICodeBlobDiff
  | IDatasetBlobDiff
  | IUnknownBlobDiff
  | IConfigBlobDiff
  | IEnvironmentBlobDiff;

export type IUnknownBlobDiff = GenericDiff<
  { category: 'unknown'; data: object },
  'unknown',
  'unknown'
>;

export interface IDeleteGenericDiff<
  Blob,
  Category extends string,
  Type extends string
> {
  category: Category;
  type: Type;
  diffType: 'deleted';
  blob: Blob;
  location: DataLocation;
}

export interface IAddGenericDiff<
  Blob,
  Category extends string,
  Type extends string
> {
  diffType: 'added';
  type: Type;
  category: Category;
  blob: Blob;
  location: DataLocation;
}

export interface IUpdateGenericDiff<
  Blob,
  Category extends string,
  Type extends string
> {
  diffType: 'updated';
  type: Type;
  category: Category;
  blobA: Blob;
  blobB: Blob;
  location: DataLocation;
}

export type GenericDiff<Blob, Category extends string, Type extends string> =
  | IDeleteGenericDiff<Blob, Category, Type>
  | IAddGenericDiff<Blob, Category, Type>
  | IUpdateGenericDiff<Blob, Category, Type>;

export const getDiffBlobs = <T extends GenericDiff<any, any, any>>(
  diff: T
): {
  blobA: T extends GenericDiff<infer Blob, any, any> ? Blob : never;
  blobB: T extends GenericDiff<infer Blob, any, any> ? Blob : never;
} => {
  const blobA = matchBy(diff as GenericDiff<any, any, any>, 'diffType')({
    added: d => undefined,
    deleted: d => d && d.blob,
    updated: d => d && d.blobA,
  });
  const blobB = matchBy(diff as GenericDiff<any, any, any>, 'diffType')({
    added: d => d && d.blob,
    deleted: d => undefined,
    updated: d => d && d.blobB,
  });
  return {
    blobA,
    blobB,
  };
};
export const getDiffBlobsData = <
  T extends GenericDiff<{ data: any }, any, any>
>(
  diff: T
): {
  blobAData: T extends GenericDiff<infer Blob, any, any>
    ? Blob extends { data: any }
      ? Blob['data']
      : never
    : never;
  blobBData: T extends GenericDiff<infer Blob, any, any>
    ? Blob extends { data: any }
      ? Blob['data']
      : never
    : never;
} => {
  const { blobA, blobB } = getDiffBlobs(diff);
  return {
    blobAData: blobA && blobA.data,
    blobBData: blobB && blobB.data,
  };
};
