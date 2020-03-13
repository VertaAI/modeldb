import * as React from 'react';

import styles from './Row.module.css';

class Row extends React.Component<any> {
  public render() {
    const { children, row, tableRow, ...restProps } = this.props;
    return (
      <tr {...restProps} className={styles.root}>
        {children}
      </tr>
    );
  }
}

export default Row;
