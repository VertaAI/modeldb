import * as React from 'react';

import styles from './NoDataCell.module.css';

interface ILocalProps {
  colSpan?: number;
  children: React.ReactNode;
}

class NoDataCell extends React.PureComponent<ILocalProps> {
  public render() {
    return (
      <td className={styles.root} colSpan={this.props.colSpan}>
        {this.props.children}
      </td>
    );
  }
}

export default NoDataCell;
