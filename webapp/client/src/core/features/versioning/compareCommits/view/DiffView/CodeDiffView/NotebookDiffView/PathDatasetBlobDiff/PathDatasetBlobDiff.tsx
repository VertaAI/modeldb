import * as React from 'react';

import { IPathDatasetComponentBlob, IPathDatasetComponentBlobDiff } from 'core/shared/models/Versioning/Blob/DatasetBlob';
import { formatBytes } from 'core/shared/utils/mapperConverters';
import matchBy from 'core/shared/utils/matchBy';

import { diffColors } from '../../../shared/styles';
import styles from './PathDatasetBlobDiff.module.css';
import Table from './Table/Table';

interface ILocalProps {
  diff: IPathDatasetComponentBlobDiff;
}

const PathDatasetBlobDiff = ({ diff }: ILocalProps) => {
  return (
    <div className={styles.root}>
      {matchBy(diff, 'diffType')({
        updated: updatedDiff => (
          <>
            <div className={styles.pathComponents}>
              <PathComponentsTable
                components={[updatedDiff.A]}
                isDeleted={true}
              />
            </div>
            <div className={styles.pathComponents}>
              <PathComponentsTable
                components={[updatedDiff.B]}
                isDeleted={false}
              />
            </div>
          </>
        ),
        added: deletedDiff => (
          <>
            <div className={styles.pathComponents}>
              <PathComponentsTable
                components={[deletedDiff.B]}
                isDeleted={false}
              />
            </div>
          </>
        ),
        deleted: deletedDiff => (
          <>
            <div className={styles.pathComponents}>
              <PathComponentsTable
                components={[deletedDiff.A]}
                isDeleted={true}
              />
            </div>
          </>
        ),
      })}
    </div>
  );
};

const PathComponentsTable = ({
  components,
  isDeleted,
}: {
  components: IPathDatasetComponentBlob[];
  isDeleted: boolean;
}) => {
  const backgroundColor = isDeleted
    ? { backgroundColor: diffColors.red }
    : { backgroundColor: diffColors.green };
  return (
    <Table data={components}>
      <Table.Column
        render={({ path }) => (
          <div className={styles.elem} style={backgroundColor} title={path}>
            {path}
          </div>
        )}
        type="path"
        title="Path"
      />
      <Table.Column
        render={({ size }) => (
          <div
            className={styles.elem}
            title={formatBytes(size)}
            style={backgroundColor}
          >
            {formatBytes(size)}
          </div>
        )}
        type="size"
        title="Size"
        width={120}
      />
      <Table.Column
        render={({ lastModifiedAtSource }) => (
          <div
            className={styles.elem}
            title={lastModifiedAtSource.toString()}
            style={backgroundColor}
          >
            {lastModifiedAtSource.toLocaleDateString() +
              ' ' +
              lastModifiedAtSource.toLocaleTimeString()}
          </div>
        )}
        type="lastModifiedAtSource"
        title="Modified"
        width={140}
      />
      <Table.Column
        render={({ md5 }) => (
          <div className={styles.elem} style={backgroundColor} title={md5}>
            {md5}
          </div>
        )}
        type="md5"
        title="MD5"
      />
    </Table>
  );
};

export default PathDatasetBlobDiff;
