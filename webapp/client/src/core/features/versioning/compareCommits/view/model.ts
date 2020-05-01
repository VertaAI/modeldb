import * as R from 'ramda';

import { ICommit } from 'core/shared/models/Versioning/RepositoryData';
import { mapObj, mapObjWithKey } from 'core/shared/utils/collection';
import {
  ComparedCommitType,
  IElementDiff,
  DiffType,
  getABData,
  getAData,
  getBData,
  GetElementData,
} from 'core/shared/models/Versioning/Blob/Diff';
import matchType from 'core/shared/utils/matchType';

import sortArrayByAnotherArrayKeys from './DiffView/shared/sortArrayByAnotherArrayKeys/sortArrayByAnotherArrayKeys';
import { diffColors } from './DiffView/shared/styles';

export interface IComparedCommitsInfo {
  commitA: Pick<ICommit, 'sha'>;
  commitB: Pick<ICommit, 'sha'>;
}

export type DiffColor = 'green' | 'red' | 'nothing';

export type IObjectToObjectWithDiffColor<T> = {
  [K in keyof T]: { value: T[K]; diffColor: DiffColor }
};

export function highlightAllProperties<T>(
  commitType: ComparedCommitType,
  obj: T
): IObjectToObjectWithDiffColor<T> {
  const diffColor = getDiffColor(commitType);
  return mapObj(
    value => ({ value, diffColor }),
    obj
  ) as IObjectToObjectWithDiffColor<T>;
}

export function highlightModifiedProperties<T>(
  commitType: ComparedCommitType,
  highlightedObj: T,
  comparedObj: T
): IObjectToObjectWithDiffColor<T> {
  return mapObjWithKey(
    (key, value) => {
      return ({
        value,
        diffColor: !R.equals(value, comparedObj[key]) ? getDiffColor(commitType) : 'nothing',
      });
    },
    highlightedObj
  ) as IObjectToObjectWithDiffColor<T>;
}

export const getDiffColor = (type: ComparedCommitType): DiffColor => {
  return type === 'A' ? 'red' : 'green';
};

export const getCssDiffColorByCommitType = (type: ComparedCommitType) => {
  return getCssDiffColor(getDiffColor(type));
};

export const getCssDiffColor = (diffColor: DiffColor) => {
  return matchType(
    {
      green: () => diffColors.green,
      red: () => diffColors.red,
      nothing: () => undefined,
    },
    diffColor
  );
};

export interface IArrayElementDiffViewModel<T> {
  data: T;
  diffColor: DiffColor;
  hightlightedPart: 'full' | 'value';
}

export interface IArrayDiffViewModel<T> {
  isHidden: boolean;
  A?: Array<IArrayElementDiffViewModel<T>>;
  B?: Array<IArrayElementDiffViewModel<T>>;
}

export interface IElementDiffDataViewModel<T> {
  data: T;
  diffColor: DiffColor;
  hightlightedPart: 'full';
}

export interface IElementDiffViewModel<T> {
  isHidden: boolean;
  A?: IElementDiffDataViewModel<T>;
  B?: IElementDiffDataViewModel<T>;
}

const getDiffsByCommitType = <T extends IElementDiff<B>, B>(
  type: ComparedCommitType,
  diffs: T[] | undefined
) => {
  const diffsByType = (diffs || []).filter(variableDiff =>
    Boolean(getABData(variableDiff)[type])
  );
  return diffsByType.length === 0 ? undefined : diffsByType;
};

const mapNullableDiffsByCommitType = <T extends IElementDiff<B>, B, R>(
  f: (data: B, diffType: DiffType, item: IElementDiff<B>) => R,
  type: ComparedCommitType,
  diffs: T[] | undefined
): R[] | undefined => {
  const diffsByType = getDiffsByCommitType(type, diffs);
  return diffsByType
    ? diffsByType.map(diff => f(getABData(diff)[type]!, diff.diffType, diff))
    : undefined;
};

export const getArrayDiffViewModel = <T>(
  getKey: (item: T) => string,
  diff: Array<IElementDiff<T>> | undefined
): IArrayDiffViewModel<T> => {
  return diff
    ? {
        isHidden: false,
        ...R.pipe(
          (diffs: IElementDiff<T>[]) => highlightDiffs(diffs),
          highlightedDiffs =>
            highlightedDiffs.A && highlightedDiffs.B
              ? putModifiedItemsSideBySide(
                  ({ data }) => getKey(data),
                  highlightedDiffs as Required<typeof highlightedDiffs>
                )
              : highlightedDiffs
        )(diff),
      }
    : {
        isHidden: true,
        A: undefined,
        B: undefined,
      };
};

const highlightDiffs = <T extends IElementDiff<any>>(
  diff: T[]
): {
  A?: Array<IArrayElementDiffViewModel<GetElementData<T>>>;
  B?: Array<IArrayElementDiffViewModel<GetElementData<T>>>;
} => {
  const A = mapNullableDiffsByCommitType(
    (data, diffType) => ({
      data: data as GetElementData<T>,
      diffColor: getDiffColor('A'),
      hightlightedPart:
        diffType === 'modified' ? ('value' as const) : ('full' as const),
    }),
    'A',
    diff
  );
  const B = mapNullableDiffsByCommitType(
    (data, diffType) => ({
      data: data as GetElementData<T>,
      diffColor: getDiffColor('B'),
      hightlightedPart:
        diffType === 'modified' ? ('value' as const) : ('full' as const),
    }),
    'B',
    diff
  );
  return { A, B };
};

const putModifiedItemsSideBySide = <T extends IArrayElementDiffViewModel<any>>(
  getKey: (item: T) => string,
  { A, B }: { A: T[]; B: T[] }
): { A: T[]; B: T[] } => {
  return {
    A: A && B ? sortArrayByAnotherArrayKeys(getKey, A, B) : A,
    B: A && B ? sortArrayByAnotherArrayKeys(getKey, B, A) : B,
  };
};

export const getElementDiffViewModel = <T extends IElementDiff<any>>(
  diff: T | undefined
): IElementDiffViewModel<GetElementData<T>> => {
  return diff
    ? {
        isHidden: false,
        A: getAData(diff)
          ? {
              diffColor: 'red' as const,
              hightlightedPart: 'full' as const,
              data: (getAData(diff)! as any) as GetElementData<T>,
            }
          : undefined,
        B: getBData(diff)
          ? {
              diffColor: 'green' as const,
              hightlightedPart: 'full' as const,
              data: (getBData(diff)! as any) as GetElementData<T>,
            }
          : undefined,
      }
    : { isHidden: true, A: undefined, B: undefined };
};
