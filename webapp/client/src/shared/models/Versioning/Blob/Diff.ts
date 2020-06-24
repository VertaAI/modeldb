import matchBy from 'shared/utils/matchBy';

import { CommitComponentLocation } from '../CommitComponentLocation';
import { ICodeBlobDiff } from './CodeBlob';
import { IConfigBlobDiff } from './ConfigBlob';
import { IDatasetBlobDiff } from './DatasetBlob';
import { IEnvironmentBlobDiff } from './EnvironmentBlob';
import matchType from 'shared/utils/matchType';

export type ComparedCommitType = 'A' | 'B' | 'C';
export type DiffType = Diff['diffType'];
export type Diff =
  | ICodeBlobDiff
  | IDatasetBlobDiff
  | IUnknownBlobDiff
  | IConfigBlobDiff
  | IEnvironmentBlobDiff;

export type IUnknownBlobDiff = IBlobDiff<
  { category: 'unknown'; data: object },
  'unknown',
  'unknown'
>;

export type IBlobDiff<Data, BlobCategory, BlobType = BlobCategory> =
  | {
      diffType: 'added';
      type: BlobType;
      category: BlobCategory;
      location: CommitComponentLocation;
      data: Data;
    }
  | {
      diffType: 'deleted';
      category: BlobCategory;
      type: BlobType;
      location: CommitComponentLocation;
      data: Data;
    }
  | {
      diffType: 'modified';
      category: BlobCategory;
      type: BlobType;
      location: CommitComponentLocation;
      data: Data;
    }
  | {
      diffType: 'conflicted';
      category: BlobCategory;
      type: BlobType;
      location: CommitComponentLocation;
      data: Data;
    };

export type IElementDiff<Element> =
  | {
      diffType: 'modified';
      A: Element;
      B: Element;
    }
  | {
      diffType: 'deleted';
      A: Element;
    }
  | {
      diffType: 'added';
      B: Element;
    }
  | {
      diffType: 'conflicted';
      A: Element;
      B: Element;
      C?: Element;
    };

export type GetElementData<
  T extends IElementDiff<any>
> = T extends IElementDiff<infer D> ? D : never;

export const elementDiffMakers = {
  modified: <Element>(A: Element, B: Element): IElementDiff<Element> => ({
    diffType: 'modified',
    A,
    B,
  }),
  added: <Element>(B: Element): IElementDiff<Element> => ({
    diffType: 'added',
    B,
  }),
  deleted: <Element>(A: Element): IElementDiff<Element> => ({
    diffType: 'deleted',
    A,
  }),
  conflicted: <Element>(
    A: Element,
    B: Element,
    C?: Element
  ): IElementDiff<Element> => ({
    diffType: 'conflicted',
    A,
    B,
    C,
  }),
};

export type IArrayDiff<Element> = Array<IElementDiff<Element>>;
export const makeArrayDiff = <T>(elems: IArrayDiff<T>): IArrayDiff<T> => elems;

export const getAData = <T extends IElementDiff<any>>(
  newDiff: T
): T extends IElementDiff<infer Element> ? Element | undefined : never => {
  return matchBy(newDiff as IElementDiff<any>, 'diffType')({
    added: diff => undefined,
    deleted: diff => diff.A,
    modified: diff => diff.A,
    conflicted: diff => diff.A,
  });
};
export const getBData = <T extends IElementDiff<any>>(
  newDiff: T
): T extends IElementDiff<infer Element> ? Element | undefined : never => {
  return matchBy(newDiff as IElementDiff<any>, 'diffType')({
    added: diff => diff.B,
    deleted: diff => undefined,
    modified: diff => diff.B,
    conflicted: diff => diff.B,
  });
};
export const getCData = <T extends IElementDiff<any>>(
  newDiff: T
): T extends IElementDiff<infer Element> ? Element | undefined : never => {
  return matchBy(newDiff as IElementDiff<any>, 'diffType')({
    added: diff => undefined,
    deleted: diff => undefined,
    modified: diff => undefined,
    conflicted: diff => diff.C,
  });
};
export const getABCData = <T extends IElementDiff<any>>(
  newDiff: T
): {
  A: T extends IElementDiff<infer Element> ? Element | undefined : never;
  B: T extends IElementDiff<infer Element> ? Element | undefined : never;
  C: T extends IElementDiff<infer Element> ? Element | undefined : never;
} => {
  return {
    A: getAData(newDiff),
    B: getBData(newDiff),
    C: getCData(newDiff),
  };
};

export type DataWithDiffTypeFromDiffs<T extends IElementDiff<any>> = {
  data: T extends IElementDiff<infer Data> ? Data : never;
  diffType: DiffType;
};
export const getCommitDataWithDiffTypeFromDiffs = <T extends IElementDiff<any>>(
  type: ComparedCommitType,
  diffs: T[]
): Array<DataWithDiffTypeFromDiffs<T>> => {
  const getData = matchType(
    {
      A: () => getAData,
      B: () => getBData,
      C: () => getCData,
    },
    type
  );

  return diffs
    .filter(diff => Boolean(getData(diff)))
    .map(diff => ({ data: getData(diff)!, diffType: diff.diffType })) as any;
};

export const getCommitDataFromNullableDiffs = <T extends IElementDiff<any>>(
  type: ComparedCommitType,
  diffs: T[] | undefined
) => {
  return diffs ? getCommitDataWithDiffTypeFromDiffs(type, diffs) : undefined;
};
