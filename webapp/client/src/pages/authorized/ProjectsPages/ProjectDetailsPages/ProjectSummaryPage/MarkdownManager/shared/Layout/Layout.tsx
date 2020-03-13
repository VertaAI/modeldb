import * as React from 'react';

import styles from './Layout.module.css';

interface ILocalProps {
  header: {
    leftContent: React.ReactNode;
    rightContent: React.ReactNode;
  };
  children: React.ReactNode;
}

class Layout extends React.PureComponent<ILocalProps> {
  public render() {
    const { header, children } = this.props;
    return (
      <div className={styles.root}>
        <div className={styles.header}>
          <div className={styles.header_leftContent}>{header.leftContent}</div>
          <div className={styles.header_rightContent}>
            {header.rightContent}
          </div>
        </div>
        <div className={styles.content}>{children}</div>
      </div>
    );
  }
}

export default Layout;
