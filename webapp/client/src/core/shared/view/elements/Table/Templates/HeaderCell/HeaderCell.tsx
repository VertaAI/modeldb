import { TemplateConnector } from '@devexpress/dx-react-core';
import * as React from 'react';

import {
  IColumnFieldSortingPluginGetters,
  IColumnFieldSortingPluginActions,
} from '../../Plugins/ColumnFieldSorting/ColumnFieldSorting';
import styles from './HeaderCell.module.css';
import SelectFieldSorting from './SelectFieldSorting/SelectFieldSorting';
import { IField } from './SelectFieldSorting/SelectFieldSorting';

const HeaderCell = ({
  column,
  tableColumn,
  showGroupingControls,
  onGroup,
  groupingEnabled,
  draggingEnabled,
  onWidthDraftCancel,
  resizingEnabled,
  onWidthChange,
  onWidthDraft,
  tableRow,
  getMessage,
  showSortingControls,
  sortingDirection,
  sortingEnabled,
  onSort,
  before,
  children,
  value,
  ...restProps
}: any) => {
  return (
    <TemplateConnector>
      {(getters, actions) => {
        const columnFieldSortingGetters = getters as IColumnFieldSortingPluginGetters;
        const columnFieldSortingActions = (actions as any) as IColumnFieldSortingPluginActions;

        const isAvailableColumnFieldSorting = Boolean(
          columnFieldSortingGetters.isAvailable
        );
        const columnFieldSortingData = (() => {
          if (isAvailableColumnFieldSorting) {
            const sortableFieldsCurrentColumn =
              columnFieldSortingGetters.sortableFieldsByColumnName[column.name];
            const withSorting =
              columnFieldSortingGetters.columnFieldSortingNames.includes(
                column.name
              ) &&
              sortableFieldsCurrentColumn &&
              sortableFieldsCurrentColumn.length !== 0;
            const selectedSorting =
              columnFieldSortingGetters.columnFieldSorting;
            return {
              withSorting,
              sortableFieldsCurrentColumn,
              selectedSorting,
            };
          }
          return undefined;
        })();

        return (
          <th className={styles.root} {...restProps} data-test="header">
            <div className={styles.cell_content}>
              {children || value}
              {(() => {
                if (
                  !columnFieldSortingData ||
                  !columnFieldSortingData.withSorting
                ) {
                  return null;
                }
                const fields: IField[] = columnFieldSortingData.sortableFieldsCurrentColumn!.map(
                  fieldName => ({
                    label: fieldName,
                    name: fieldName,
                  })
                );
                return (
                  <div className={styles.selectSortField}>
                    <SelectFieldSorting
                      fields={fields}
                      selected={
                        columnFieldSortingData.selectedSorting &&
                        columnFieldSortingData.selectedSorting.columnName ===
                          column.name
                          ? columnFieldSortingData.selectedSorting
                          : null
                      }
                      columnName={column.name}
                      onChange={
                        columnFieldSortingActions.changeColumnFieldSorting
                      }
                    />
                  </div>
                );
              })()}
            </div>
            {columnFieldSortingData &&
              columnFieldSortingData.selectedSorting &&
              columnFieldSortingData.selectedSorting.columnName ===
                column.name && (
                <div
                  className={styles.sorting_field}
                  title={columnFieldSortingData.selectedSorting.fieldName}
                >
                  SortedBy:{' '}
                  <span className={styles.sorting_value}>
                    {columnFieldSortingData.selectedSorting.fieldName}
                  </span>{' '}
                  (
                  {columnFieldSortingData.selectedSorting.direction === 'asc'
                    ? 'Asc'
                    : 'Desc'}
                  )
                </div>
              )}
          </th>
        );
      }}
    </TemplateConnector>
  );
};

export default HeaderCell;
