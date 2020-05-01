import * as React from 'react';

import { IPathDatasetComponentBlob } from 'core/shared/models/Versioning/Blob/DatasetBlob';
import PathSize from 'core/shared/view/domain/Versioning/Blob/DatasetBlob/PathSize/PathSize';

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
            <PathSize className={styles.elem} size={size} />
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
