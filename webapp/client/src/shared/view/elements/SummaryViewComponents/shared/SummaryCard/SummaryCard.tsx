import * as React from 'react';

import styles from './SummaryCard.module.css';

interface ILocalProps {
  title: string;
  children: React.ReactNode;
}

class SummaryCard extends React.PureComponent<ILocalProps> {
  public render() {
    const { title, children } = this.props;

    return (
      <div className={styles.root}>
        <div className={styles.title}>{title}</div>
        <div className={styles.content}>{children}</div>
      </div>
    );
  }
}

export default SummaryCard;
