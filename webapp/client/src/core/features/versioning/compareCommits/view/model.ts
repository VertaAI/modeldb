import { ICommit } from 'core/shared/models/Versioning/RepositoryData';
import { mapObj, mapObjWithKey } from 'core/shared/utils/collection';
import { ComparedCommitType } from 'core/shared/models/Versioning/Blob/Diff';
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
  const diffColor = getDiffColor(commitType);
  return mapObjWithKey(
    (key, value) => ({
      value,
      diffColor: value !== comparedObj[key] ? diffColor : 'nothing',
    }),
    highlightedObj
  ) as IObjectToObjectWithDiffColor<T>;
}

export const getDiffColor = (type: ComparedCommitType): DiffColor => {
  return type === 'A' ? 'red' : 'green';
};

export const getCssDiffColor = (type: ComparedCommitType) => {
  return getDiffColor(type) === 'red' ? diffColors.red : diffColors.green;
};
