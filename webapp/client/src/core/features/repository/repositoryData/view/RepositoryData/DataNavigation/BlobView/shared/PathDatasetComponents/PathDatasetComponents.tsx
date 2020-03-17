import * as React from 'react';

import { IPathDatasetComponentBlob } from 'core/shared/models/Repository/Blob/DatasetBlob';
import { formatBytes } from 'core/shared/utils/mapperConverters';

import styles from './PathDatasetComponents.module.css';
import Table from './Table/Table';

interface ILocalProps {
  data: IPathDatasetComponentBlob[];
}

const PathDatasetComponents = (props: ILocalProps) => {
  return (
    <div className={styles.root}>
      <Table data={props.data}>
        <Table.Column
          render={({ path }) => (
            <span className={styles.elem} title={path}>
              {path}
            </span>
          )}
          type="path"
          title="Path"
        />
        <Table.Column
          render={({ size }) => (
            <span className={styles.elem} title={formatBytes(size)}>
              {formatBytes(size)}
            </span>
          )}
          type="size"
          title="Size"
          width={120}
        />
        <Table.Column
          render={({ lastModifiedAtSource }) => (
            <span
              className={styles.elem}
              title={lastModifiedAtSource.toString()}
            >
              {lastModifiedAtSource.toLocaleDateString() +
                ' ' +
                lastModifiedAtSource.toLocaleTimeString()}
            </span>
          )}
          type="lastModifiedAtSource"
          title="Modified"
          width={140}
        />
        <Table.Column
          render={({ md5 }) => (
            <span className={styles.elem} title={md5}>
              {md5}
            </span>
          )}
          type="md5"
          title="MD5"
        />
      </Table>
    </div>
  );
};

export default PathDatasetComponents;
