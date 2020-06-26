import cn from 'classnames';
import React from 'react';

import { getAlignClassname } from './helpers';
import styles from './styles.module.css';
import { ICell, IRow, IAdditionalClassNames, ISelection } from './types';

export function NoData({
  noData = () => 'No data',
}: {
  noData?: () => React.ReactNode;
}) {
  return (
    <div className={styles.noData} data-test="no-data">
      {noData()}
    </div>
  );
}

export function Row<T>({
  row,
  additionalClassNames,
  selection,
  onClick,
}: {
  row: IRow<T>;
  additionalClassNames: IAdditionalClassNames;
  selection?: ISelection<T>;
  onClick?: () => void;
}) {
  return (
    <div className={styles.rowWrapper} onClick={onClick}>
      <div className={styles.selectionCell}>
        {selection && selection.showSelectionColumn
          ? selection.cellComponent(row.data)
          : null}
      </div>
      <div
        className={cn(styles.row, additionalClassNames.row)}
        style={row.style}
      >
        {row.cells.map((cell, index) => (
          <Cell
            key={cell.key}
            cell={cell}
            rowData={row.data}
            additionalClassNames={additionalClassNames}
          />
        ))}
      </div>
    </div>
  );
}

export function Cell<T>({
  cell,
  additionalClassNames,
  rowData,
}: {
  rowData: T;
  cell: ICell<T>;
  additionalClassNames: IAdditionalClassNames;
}) {
  return (
    <div
      className={cn(
        styles.cell,
        additionalClassNames.cell,
        getAlignClassname(cell.align)
      )}
      data-type={cell.dataType}
      style={cell.style}
    >
      {cell.render(rowData)}
    </div>
  );
}
