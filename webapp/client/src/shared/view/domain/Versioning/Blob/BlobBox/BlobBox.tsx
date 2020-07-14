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
      <div className={styles.title}>
        <BlobTitle title={title} />
      </div>
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
    <div className={styles.multipleBlobBox}>
      <div className={styles.title}>
        <BlobTitle title={title} />
      </div>
      <div className={styles.boxes}>
        {React.Children.toArray(children)
          .filter(Boolean)
          .map((child, i) => (
            <div className={styles.blobBox} key={i}>
              {child}
            </div>
          ))}
      </div>
    </div>
  );
};

export { BlobDataBox, MultipleBlobDataBox, DataBox };
