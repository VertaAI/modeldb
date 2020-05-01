import cn from 'classnames';
import * as React from 'react';
import { Table, Column } from '@devexpress/dx-react-grid';

import styles from './Cell.module.css';

interface IAdditionalCellProps<Row, ColumnName = string> {
  className?: string;
  getStyle?: (column: Omit<Column, 'name'> & { name: ColumnName }, row: Row) => React.CSSProperties | undefined;
  getDataType?: (column: Omit<Column, 'name'> & { name: ColumnName }, row: Row) => string | undefined;
  type?: string;
}

export function makeGenericCell<Row, ColumnName = string>(): (props: Table.DataCellProps & IAdditionalCellProps<Row, ColumnName>) => any {
  return Cell as any;
}

class Cell<Row, ColumnName = string> extends React.Component<Table.DataCellProps & IAdditionalCellProps<Row, ColumnName>> {
  public render() {
    const {
      column,
      value,
      children,
      tableRow,
      tableColumn,
      row,
      className,
      getStyle = () => undefined,
      getDataType = () => undefined,
      ...restProps
    } = this.props;
    return (
      <td
        className={cn(
          styles.root,
          className,
        )}
        {...restProps}
        style={getStyle({
          ...column,
          name: column.name as any,
        }, row)}
        align={tableColumn.align}
        data-name={column.name}
        data-type={getDataType(column as any, row)}
      >
        {children || value}
      </td>
    );
  }
}

export default Cell;