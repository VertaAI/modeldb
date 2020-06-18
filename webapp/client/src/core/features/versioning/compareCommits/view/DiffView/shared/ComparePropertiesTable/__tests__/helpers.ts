import { ReactWrapper } from 'enzyme';

import { ComparedCommitType } from 'core/shared/models/Versioning/Blob/Diff';

import { DiffColor } from '../../../../model';
import { diffColors } from '../../styles';

export const getCommitColumns = (
  type: ComparedCommitType,
  component: ReactWrapper
) => {
  return component.find(`[data-name="${type}"]`);
};

export function getCommitColumnInfo<T>(
  commitType: ComparedCommitType,
  propType: string,
  getContent: (column: ReactWrapper) => T,
  component: ReactWrapper
): {
  content: undefined | T;
  diffColor: undefined | DiffColor;
} {
  const targetColumn = getCommitColumns(commitType, component).findWhere(
    column => column.prop('data-type') === propType
  );
  if (targetColumn.length === 0) {
    return { content: undefined, diffColor: undefined };
  }
  return {
    content: getContent(targetColumn),
    diffColor: getColumnDiffColor(targetColumn),
  };
}

export const getColumnDiffColor = (column: ReactWrapper) =>
  getDiffColorFromBackgroundColor(column.prop('style'));

export const getDiffColorFromBackgroundColor = (
  style: React.CSSProperties | undefined
): DiffColor | undefined => {
  const backgroundColor = style && style.backgroundColor;
  return backgroundColor === diffColors.aDiff
    ? 'aDiff'
    : backgroundColor === diffColors.bDiff
    ? 'bDiff'
    : undefined;
};
