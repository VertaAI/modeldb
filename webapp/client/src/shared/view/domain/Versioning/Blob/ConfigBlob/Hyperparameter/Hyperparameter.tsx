import * as React from 'react';

import styles from './Hyperparameter.module.css';

interface ILocalProps {
  name: string;
  children: React.ReactNode;
  rootStyles?: React.CSSProperties;
  valueStyles?: React.CSSProperties;
}

const Hyperparameter = ({
  name,
  children,
  rootStyles,
  valueStyles,
}: ILocalProps) => {
  return (
    <div className={styles.root} data-test="hyperparameter" style={rootStyles}>
      <span className={styles.name} title={name} data-test="name">
        {name}
      </span>
      <span className={styles.value} data-test="value" style={valueStyles}>
        {children}
      </span>
    </div>
  );
};

export default Hyperparameter;
