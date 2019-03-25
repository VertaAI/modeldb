import * as React from 'react';

import styles from './FormItem.module.css';

interface IProps {
  label: string;
  children: React.ReactNode;
}

// todo rename
const FormItem = ({ label, children }: IProps) => {
  return (
    <div className={styles.form_item}>
      <span className={styles.label}>{label}</span>
      <div className={styles.content}>{children}</div>
    </div>
  );
};

export default FormItem;
