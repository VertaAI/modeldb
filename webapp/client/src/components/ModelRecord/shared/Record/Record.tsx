import cn from 'classnames';
import React from 'react';

import styles from './Record.module.css';

interface ILocalProps {
  label: string;
  children?: React.ReactNode;
  additionalContainerClassName?: string;
  additionalHeaderClassName?: string;
  additionalValueClassName?: string;
}

const Record: React.FC<ILocalProps> = ({
  label,
  children,
  additionalContainerClassName = '',
  additionalHeaderClassName = '',
  additionalValueClassName = '',
}) => {
  return (
    <div className={cn(styles.root, additionalContainerClassName)}>
      <div className={cn(styles.label, additionalHeaderClassName)}>{label}</div>
      <div className={cn(styles.value, additionalValueClassName)}>
        <span>{children}</span>
      </div>
    </div>
  );
};

export default Record;
