import cn from 'classnames';
import * as React from 'react';

import InlineErrorView from '../Errors/InlineErrorView/InlineErrorView';
import styles from './FieldWithTopLabel.module.css';

const FieldWithTopLabel = ({
  label,
  isRequired,
  children,
  meta,
  dataTest,
}: {
  label: string;
  meta?: { touched: boolean; error?: string };
  isRequired?: boolean;
  dataTest?: string;
  children: React.ReactNode;
}) => {
  return (
    <div className={cn(styles.labeledField, { [styles.required]: isRequired })}>
      <div className={styles.labeledField__label}>{label}</div>
      <div className={styles.labeledField__control}>{children}</div>
      {meta && meta.touched && meta.error && (
        <InlineErrorView
          error={meta.error}
          dataTest={`${dataTest || 'field'}-error`}
        />
      )}
    </div>
  );
};

export default FieldWithTopLabel;
