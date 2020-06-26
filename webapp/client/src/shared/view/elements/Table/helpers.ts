import * as R from 'ramda';

import matchType from 'shared/utils/matchType';

import styles from './styles.module.css';
import {
  ColumnDefinition,
  IRowStyle,
  ColumnAlign,
  ICommonColumnDefinition,
} from './types';

export function getRowStylesByColumnDefinitions<T>(
  columnDefinitions: Array<ColumnDefinition<T>>
): IRowStyle {
  return {
    display: 'grid',
    gridTemplateColumns: columnDefinitions.map((c) => c.width).join(' '),
  };
}

export function isSortedColumn<T>(columnDefinition: ColumnDefinition<T>) {
  if ('withSort' in columnDefinition && columnDefinition.withSort) {
    return true;
  }
  return false;
}

export const getAlignClassname = (align: ColumnAlign) => {
  if (!align) {
    return styles.align_left;
  }

  return matchType(
    {
      center: () => styles.align_center,
      left: () => styles.align_left,
      right: () => styles.align_right,
    },
    align
  );
};

export const checkColumnWidthsInPercentage = (
  columnDefs: Array<ICommonColumnDefinition<any>>
) => {
  if (process.env.NODE_ENV === 'production') {
    return;
  }
  if (columnDefs.every(({ width }) => /\d+%/.test(width))) {
    const columnsWidth = R.sum(
      columnDefs.map(({ width }) => parseFloat(width))
    );
    console.assert(
      columnsWidth === 100,
      `sum of column widths is not equal 100. Actual value = ${columnsWidth}`
    );
  }
};
