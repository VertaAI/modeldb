import cn from 'classnames';
import * as React from 'react';

import styles from './TableWrapper.module.css';

interface ILocalProps {
  dataTest?: string;
  isHeightByContent?: boolean;
  children: React.ReactNode;
}

class TableWrapper extends React.PureComponent<ILocalProps> {
  public render() {
    const { children, dataTest, isHeightByContent } = this.props;

    return (
      <div
        className={cn(styles.root, {
          [styles.heightByContent]: isHeightByContent,
        })}
        data-test={dataTest}
      >
        {children}
      </div>
    );
  }
}

export default TableWrapper;
