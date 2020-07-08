import cn from 'classnames';
import React, { useMemo } from 'react';

import { Row, NoData } from '../components';
import {
  getRowStylesByColumnDefinitions,
  checkColumnWidthsInPercentage,
} from '../helpers';
import { useSortInfo } from '../Sorting/sorting';
import { SortInfo } from '../Sorting/types';
import styles from '../styles.module.css';
import { ICommonTableProps, getTableRows } from '../Table';
import { TableHeader } from '../TableHeader';
import {
  ColumnDefinition,
  DataGroup,
  IRowStyle,
  IGroup,
  IAdditionalClassNames,
  ISelection,
} from '../types';

interface IGrouppedTableProps<T> extends ICommonTableProps<T> {
  dataGroups: Array<DataGroup<T>>;
  getGroupStyle?: (group: DataGroup<T>) => object;
  getGroupKey: (group: DataGroup<T>, index: number) => string | number;
}

function getTableGroups<T>({
  dataGroups,
  columnDefinitions,
  rowStyle,
  getGroupStyle,
  sortInfo,
  getGroupKey,
  getRowKey,
}: {
  sortInfo: SortInfo;
  dataGroups: Array<DataGroup<T>>;
  columnDefinitions: Array<ColumnDefinition<T>>;
  rowStyle: IRowStyle;
  getGroupStyle?: (group: DataGroup<T>) => object;
  getGroupKey: (group: DataGroup<T>, index: number) => string | number;
  getRowKey: (row: T, index: number) => string | number;
}): Array<IGroup<T>> {
  return dataGroups.map((dataGroup, index) => ({
    rows: getTableRows({
      dataRows: dataGroup,
      columnDefinitions,
      rowStyle,
      sortInfo,
      getRowKey,
    }),
    style: getGroupStyle ? getGroupStyle(dataGroup) : {},
    key: getGroupKey(dataGroup, index),
  }));
}

function GrouppedTable<T>(props: IGrouppedTableProps<T>) {
  const {
    columnDefinitions,
    additionalClassNames = {},
    dataTest,
    selection,
    noData,
    getRowKey,
    getGroupKey,
    dataGroups,
    getGroupStyle,
  } = props;

  const [sortInfo, changeSortedType] = useSortInfo();

  checkColumnWidthsInPercentage(columnDefinitions);

  const rowStyle = useMemo(
    () => getRowStylesByColumnDefinitions(columnDefinitions),
    [columnDefinitions]
  );

  const tableGroups = useMemo(
    () =>
      getTableGroups({
        dataGroups,
        columnDefinitions,
        rowStyle,
        getGroupStyle,
        sortInfo,
        getRowKey,
        getGroupKey,
      }),
    [
      dataGroups,
      getGroupStyle,
      columnDefinitions,
      rowStyle,
      sortInfo,
      getRowKey,
      getGroupKey,
    ]
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
          isNoData={tableGroups.length === 0}
        />

        {tableGroups.length === 0 && <NoData noData={noData} />}

        {tableGroups.map(group => (
          <Group<T>
            group={group}
            key={group.key}
            additionalClassNames={additionalClassNames}
            selection={selection}
          />
        ))}
      </div>
    </div>
  );
}

export function Group<T>({
  group,
  additionalClassNames,
  selection,
}: {
  group: IGroup<T>;
  additionalClassNames: IAdditionalClassNames;
  selection?: ISelection<T>;
}) {
  return (
    <div
      className={cn(styles.group, additionalClassNames.group)}
      style={group.style}
    >
      {group.rows.map((row, index) => (
        <Row
          key={row.key}
          row={row}
          additionalClassNames={additionalClassNames}
          selection={selection}
        />
      ))}
    </div>
  );
}

export default GrouppedTable;
