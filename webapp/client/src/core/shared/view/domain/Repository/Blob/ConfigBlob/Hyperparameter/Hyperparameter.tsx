import cn from 'classnames';
import * as React from 'react';

import { IConfigHyperparameter } from 'core/shared/models/Repository/Blob/ConfigBlob';

import HyperparameterValue from '../HyperparameterValue/HyperparameterValue';
import styles from './Hyperparameter.module.css';

interface ILocalProps {
  name: string;
  children: React.ReactNode;
  rootStyles?: React.CSSProperties;
}

const Hyperparameter = ({ name, children, rootStyles }: ILocalProps) => {
  return (
    <div className={styles.root} style={rootStyles}>
      <span className={styles.name} title={name}>
        {name}
      </span>
      <span className={styles.value}>{children}</span>
    </div>
  );
};

export default Hyperparameter;
