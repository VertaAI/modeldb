import * as React from 'react';

import styles from './Placeholder.module.css';

interface ILocalProps {
  dataTest?: string;
  children: React.ReactNode;
}

const Placeholder = (props: ILocalProps) => {
  return (
    <div className={styles.root} data-test={props.dataTest}>
      {props.children}
    </div>
  );
};

export default Placeholder;
