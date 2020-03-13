import cn from 'classnames';
import * as React from 'react';

import styles from './Cell.module.css';

class Cell extends React.Component<any> {
  public render() {
    const {
      column,
      value,
      children,
      tableRow,
      tableColumn,
      row,
      className,
      ...restProps
    } = this.props;
    return (
      <td
        className={cn(styles.root, className)}
        {...restProps}
        data-name={column.name}
      >
        {children || value}
      </td>
    );
  }
}

export default Cell;
