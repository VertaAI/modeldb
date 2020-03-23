import React from 'react';

import CopyButton from 'core/shared/view/elements/CopyButton/CopyButton';
import IdView from 'core/shared/view/elements/IdView/IdView';

import styles from './ModelMeta.module.css';

export const ModelMeta = (props: {
  label: string;
  children: React.ReactNode;
  valueTitle: string;
  copy?: boolean;
  id?: string;
  runId?: boolean;
}) => {
  const { id, label, valueTitle, children } = props;
  return (
    <div className={styles.meta_container}>
      <div className={styles.meta_label_container} title={label}>
        {label}
      </div>
      <div className={styles.value_container}>
        <div className={styles.meta_value} title={valueTitle}>
          {children}
        </div>
      </div>
    </div>
  );
};

export const ModelIdMeta = (props: {
  label: string;
  children: React.ReactNode;
  valueTitle: string;
  copy?: boolean;
  id?: string;
  runId?: boolean;
}) => {
  const { id, label, valueTitle, children, copy, runId } = props;
  return (
    <div className={styles.meta_id_container}>
      <div className={styles.meta_label_container} title={label}>
        {label}
      </div>
      <div className={styles[copy ? 'value_id_container' : 'value_container']}>
        <div className={styles.meta_value} title={valueTitle}>
          {children}
        </div>
        {copy ? (
          <div>
            (
            {id && !runId ? (
              <span title={id}>
                <IdView value={id.slice(0, 7)} />,{' '}
              </span>
            ) : (
              ''
            )}
            <CopyButton value={id || String(children)} />)
          </div>
        ) : (
          ''
        )}
      </div>
    </div>
  );
};
