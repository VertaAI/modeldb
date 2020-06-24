import cn from 'classnames';
import React, { useMemo } from 'react';

import { NoData, Row } from './components';
import {
  getRowStylesByColumnDefinitions,
  checkColumnWidthsInPercentage,
} from './helpers';
import { useSortInfo, sortDataRows } from './Sorting/sorting';
import { SortInfo } from './Sorting/types';
import styles from './styles.module.css';
import { TableHeader } from './TableHeader';
import {
  ColumnDefinition,
  IAdditionalClassNames,
  ISelection,
  IRowStyle,
  IRow,
} from './types';

export interface ICommonTableProps<T> {
  columnDefinitions: Array<ColumnDefinition<T>>;
  additionalClassNames?: IAdditionalClassNames;
  dataTest?: string;
  selection?: ISelection<T>;
  noData?: () => React.ReactNode;
  getRowKey: (row: T, index: number) => string | number;
  onRowClick?: (row: T, index: number) => void;
}

interface ITableProps<T> extends ICommonTableProps<T> {
  dataRows: T[];
}

export function getTableRows<T>({
  dataRows,
  columnDefinitions,
  rowStyle,
  sortInfo,
  getRowKey,
}: {
  dataRows: T[];
  columnDefinitions: Array<ColumnDefinition<T>>;
  rowStyle: IRowStyle;
  sortInfo: SortInfo;
  getRowKey: (row: T, index: number) => string | number;
}): Array<IRow<T>> {
  return sortDataRows({
    dataRows,
    columnDefinitions,
    sortInfo,
  }).map((dataRow, index) => ({
    data: dataRow,
    style: rowStyle,
    key: getRowKey(dataRow, index),
    cells: columnDefinitions.map(column => ({
      render: column.render,
      style: column.getCellStyle ? column.getCellStyle(dataRow) : undefined,
      dataType: column.type.toString(),
      key: column.type,
      align: column.align,
    })),
  }));
}

export default function Table<T>(props: ITableProps<T>) {
  const {
    columnDefinitions,
    additionalClassNames = {},
    dataTest,
    selection,
    noData,
    getRowKey,
    dataRows,
    onRowClick,
  } = props;

  checkColumnWidthsInPercentage(columnDefinitions);

  const [sortInfo, changeSortedType] = useSortInfo();

  const rowStyle = useMemo(
    () => getRowStylesByColumnDefinitions(columnDefinitions),
    [columnDefinitions]
  );

  const tableRows = useMemo(
    () =>
      getTableRows({
        dataRows,
        columnDefinitions,
        rowStyle,
        sortInfo,
        getRowKey,
      }),
    [dataRows, columnDefinitions, sortInfo, getRowKey, rowStyle]
  );

  return (
    <div
      className={cn(styles.tableRoot, additionalClassNames.root)}
      data-test={dataTest}
    >
      <div>
        <TableHeader<T>
          additionalClassNames={additionalClassNames}
          columnDefinitions={columnDefinitions}
          rowStyles={rowStyle}
          changeSortedType={changeSortedType}
          sortInfo={sortInfo}
          selection={selection}
          isNoData={tableRows.length === 0}
        />

        {tableRows.length === 0 && <NoData noData={noData} />}

        {tableRows.map((row, index) => (
          <Row<T>
            row={row}
            key={row.key}
            additionalClassNames={additionalClassNames}
            selection={selection}
            onClick={() => onRowClick && onRowClick(row.data, index)}
          />
        ))}
      </div>
    </div>
  );
}
