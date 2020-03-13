import cn from 'classnames';
import * as React from 'react';

import styles from './TableWrapper.module.css';

interface ILocalProps {
  dataTest?: string;
  children: React.ReactNode;
}

class TableWrapper extends React.PureComponent<ILocalProps> {
  public render() {
    const { children, dataTest } = this.props;

    return (
      <div className={cn(styles.root)} data-test={dataTest}>
        {children}
      </div>
    );
  }
}

export default TableWrapper;
