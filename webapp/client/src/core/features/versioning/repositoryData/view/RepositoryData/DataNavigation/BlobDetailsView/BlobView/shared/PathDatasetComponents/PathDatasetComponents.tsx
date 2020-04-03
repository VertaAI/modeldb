import * as React from 'react';

import { IPathDatasetComponentBlob } from 'core/shared/models/Versioning/Blob/DatasetBlob';
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
          title="Path"
          type="path"
          width={250}
          render={({ path }) => (
            <span className={styles.elem} title={path}>
              {path}
            </span>
          )}
        />
        <Table.Column
          title="Size"
          type="size"
          width={120}
          render={({ size }) => (
            <span className={styles.elem} title={formatBytes(size)}>
              {formatBytes(size)}
            </span>
          )}
        />
        <Table.Column
          title="Modified"
          type="lastModifiedAtSource"
          width={170}
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
        />
        <Table.Column
          title="MD5"
          type="md5"
          width={150}
          render={({ md5 }) => (
            <span className={styles.elem} title={md5}>
              {md5}
            </span>
          )}
        />
        <Table.Column
          title="SHA256"
          type="sha256"
          render={({ sha256 }) => (
            <span className={styles.elem} title={sha256}>
              {sha256}
            </span>
          )}
        />
      </Table>
    </div>
  );
};

export default PathDatasetComponents;
