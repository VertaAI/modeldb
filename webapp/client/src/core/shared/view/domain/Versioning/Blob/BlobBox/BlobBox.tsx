import * as React from 'react';
import cn from 'classnames';

import styles from './BlobBox.module.css';
import BlobTitle from '../BlobTitle/BlobTitle';

const DataBox = (props: {
  withPadding: boolean;
  additionalClassname?: string;
  children: React.ReactNode;
}) => {
  return (
    <div
      className={cn(styles.dataBox, props.additionalClassname, {
        [styles.withPadding]: props.withPadding,
      })}
    >
      {props.children}
    </div>
  );
};

const BlobDataBox = ({
  title,
  children,
}: {
  title: string;
  children: React.ReactNode;
}) => {
  return (
    <div className={styles.root}>
      <BlobTitle title={title} />
      <div className={styles.content}>{children}</div>
    </div>
  );
};

const MultipleBlobDataBox = ({
  title,
  children,
}: {
  title: string;
  children: React.ReactNodeArray | React.ReactFragment;
}) => {
  return (
    <div className={styles.root}>
      <BlobTitle title={title} />
      <div className={styles.boxes}>
        {React.Children.toArray(children)
          .filter(Boolean)
          .map(child => (
            <div className={styles.blobBox}>{child}</div>
          ))}
      </div>
    </div>
  );
};

export { BlobDataBox, MultipleBlobDataBox, DataBox };
