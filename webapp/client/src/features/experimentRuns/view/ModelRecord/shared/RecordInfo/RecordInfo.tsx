import React from 'react';
import cn from 'classnames';

import styles from './RecordInfo.module.css';

export const RecordInfo = (props: {
  label: string;
  labelAlign?: 'top' | 'center';
  children: React.ReactNode;
  valueTitle?: string;
  withValueTruncation?: boolean;
}) => {
  const {
    label,
    labelAlign = 'top',
    valueTitle,
    children,
    withValueTruncation = true,
  } = props;
  return (
    <div className={styles.root}>
      <div
        className={cn(styles.label, {
          [styles.align_top]: labelAlign === 'top',
          [styles.align_center]: labelAlign === 'center',
        })}
        title={label}
      >
        {label}
      </div>
      <div
        className={cn(styles.value, {
          [styles.withTruncation]: withValueTruncation,
        })}
        title={valueTitle}
      >
        {children}
      </div>
    </div>
  );
};
