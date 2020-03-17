import * as React from 'react';

import styles from './DataBox.module.css';

interface ILocalProps {
  children: React.ReactNode;
}

const DataBox = (props: ILocalProps) => {
  return <div className={styles.root}>{props.children}</div>;
};

export default DataBox;
