import * as React from 'react';

import styles from './Badge.module.css';

interface ILocalProps {
  count: number;
}

const Badge = ({ count }: ILocalProps) => {
  return <div className={styles.root}>{count}</div>;
};

export default Badge;
