import cn from 'classnames';
import React, { useCallback } from 'react';

import { isSortedColumn, getAlignClassname } from './helpers';
import { SortLabel } from './Sorting/components';
import { useLabelDirection } from './Sorting/helpers';
import { SortInfo } from './Sorting/types';
import styles from './styles.module.css';
import {
  ColumnDefinition,
  IRowStyle,
  IAdditionalClassNames,
  ISelection,
  ColumnAlign,
  CustomSortLabel,
} from './types';

export function TableHeader<T>({
  columnDefinitions,
  rowStyles,
  additionalClassNames,
  changeSortedType,
  sortInfo,
  selection,
  isNoData,
}: {
  columnDefinitions: Array<ColumnDefinition<T>>;
  rowStyles: IRowStyle;
  additionalClassNames: IAdditionalClassNames;
  changeSortedType: (type: string) => void;
  sortInfo: SortInfo;
  selection?: ISelection<T>;
  isNoData: boolean;
}) {
  return (
    <div className={cn(styles.rowWrapper, styles.headerRowWrapper)}>
      <div className={styles.selectionCell}>
        {selection && selection.showSelectAll
          ? selection.headerCellComponent()
          : null}
      </div>
      <div
        className={cn(
          styles.row,
          additionalClassNames.headerRow,
          styles.header
        )}
        style={rowStyles}
      >
        {columnDefinitions.map(c => (
          <HeaderCell
            key={c.type}
            title={c.title}
            type={c.type}
            additionalClassNames={additionalClassNames}
            changeSortedType={changeSortedType}
            sortInfo={sortInfo}
            isSortLabelVisible={Boolean(isSortedColumn(c)) && !isNoData}
            customSortLabel={c.withSort ? c.customSortLabel : undefined}
            align={c.align}
          />
        ))}
      </div>
    </div>
  );
}

export const HeaderCell = ({
  additionalClassNames,
  changeSortedType,
  sortInfo,
  title,
  type,
  isSortLabelVisible,
  customSortLabel,
  align,
}: {
  additionalClassNames: IAdditionalClassNames;
  title: string;
  type: string;
  changeSortedType: (type: string) => void;
  sortInfo: SortInfo;
  isSortLabelVisible: boolean;
  customSortLabel?: CustomSortLabel;
  align: ColumnAlign;
}) => {
  const onSort = useCallback(() => changeSortedType(type), [
    changeSortedType,
    type,
  ]);
  const direction = useLabelDirection({
    sortInfo,
    type,
  });
  return (
    <div
      className={cn(
        styles.headerCell,
        additionalClassNames.headerCell,
        getAlignClassname(align)
      )}
      data-test={`header-${type}`}
    >
      {isSortLabelVisible ? (
        <SortLabel
          onSort={onSort}
          direction={direction}
          customSortLabel={customSortLabel}
        >
          {title}
        </SortLabel>
      ) : (
        title
      )}
    </div>
  );
};
