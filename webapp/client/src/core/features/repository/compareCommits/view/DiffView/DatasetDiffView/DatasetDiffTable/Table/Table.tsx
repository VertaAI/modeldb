import { Column, DataTypeProvider } from '@devexpress/dx-react-grid';
import {
  Grid,
  Table as DevExpessTable,
  TableHeaderRow,
} from '@devexpress/dx-react-grid-material-ui';
import Paper from '@material-ui/core/Paper';
import cn from 'classnames';
import React from 'react';

import { IPathDatasetComponentBlob } from 'core/shared/models/Repository/Blob/DatasetBlob';
import { DiffType } from 'core/shared/models/Repository/Blob/Diff';
import { formatBytes } from 'core/shared/utils/mapperConverters';

import styles from './Table.module.css';

interface ILocalProps {
  rows: IRow[];
}

export interface IRow {
  data: IPathDatasetComponentBlob;
  diffInfo: IDiffInfo;
  isFirstCommit: boolean;
}

type IDiffInfo =
  | { status: 'deleted' }
  | { status: 'added' }
  | {
      status: 'updated';
      data: Record<keyof IPathDatasetComponentBlob, boolean>;
    };

enum ColumnName {
  path = 'path',
  size = 'size',
  md5 = 'md5',
  lastModifiedAtSource = 'lastModifiedAtSource',
  sha256 = 'sha256',
}

const columns: Column[] = [
  { name: ColumnName.path, title: 'Path' },
  { name: ColumnName.size, title: 'Size' },
  { name: ColumnName.lastModifiedAtSource, title: 'Modified' },
  { name: ColumnName.md5, title: 'MD5' },
  { name: ColumnName.sha256, title: 'SHA256' },
];
const tableColumnExtensions: DevExpessTable.ColumnExtension[] = [
  { columnName: ColumnName.path, width: 250, align: 'left' },
  { columnName: ColumnName.size, width: 120, align: 'center' },
  { columnName: ColumnName.lastModifiedAtSource, width: 170, align: 'center' },
  { columnName: ColumnName.md5, width: 150, align: 'center' },
  { columnName: ColumnName.sha256, width: 150, align: 'center' },
];

const getCeilClassname = (row: IRow, fieldName: keyof IRow['data']) => {
  return cn(styles.elem, {
    [styles.deleted]:
      row.diffInfo.status === 'deleted' ||
      (row.diffInfo.status === 'updated' &&
        row.isFirstCommit &&
        row.diffInfo.data[fieldName]),
    [styles.added]:
      row.diffInfo.status === 'added' ||
      (row.diffInfo.status === 'updated' &&
        !row.isFirstCommit &&
        row.diffInfo.data[fieldName]),
  });
};

const Root = (props: any) => <Grid.Root {...props} style={{ width: '100%' }} />;

const Row = (props: { row: IRow; [key: string]: any }) => {
  const { children, row, tableRow, ...restProps } = props;

  return (
    <tr
      className={cn({
        [styles.deleted]: row.diffInfo.status === 'deleted',
        [styles.added]: row.diffInfo.status === 'added',
      })}
      {...restProps}
    >
      {children}
    </tr>
  );
};

const Table: React.FC<ILocalProps> = ({ rows }) => {
  return (
    <div className={styles.root}>
      <Paper>
        <Grid rows={rows} columns={columns} rootComponent={Root}>
          <DataTypeProvider
            for={[ColumnName.path]}
            formatterComponent={({ row }) => (
              <div
                className={getCeilClassname(row, 'path')}
                title={row.data.path}
              >
                {row.data.path}
              </div>
            )}
          />
          <DataTypeProvider
            for={[ColumnName.size]}
            formatterComponent={({ row }) => (
              <div
                className={getCeilClassname(row, 'size')}
                title={row.data.size}
              >
                {formatBytes(row.data.size)}
              </div>
            )}
          />
          <DataTypeProvider
            for={[ColumnName.md5]}
            formatterComponent={({ row }) => (
              <div
                className={getCeilClassname(row, 'md5')}
                title={row.data.md5}
              >
                {row.data.md5}
              </div>
            )}
          />
          <DataTypeProvider
            for={[ColumnName.lastModifiedAtSource]}
            formatterComponent={({ row }) => (
              <div
                className={getCeilClassname(row, 'lastModifiedAtSource')}
                title={row.data.lastModifiedAtSource}
              >
                {row.data.lastModifiedAtSource.toLocaleDateString() +
                  ' ' +
                  row.data.lastModifiedAtSource.toLocaleTimeString()}
              </div>
            )}
          />
          <DataTypeProvider
            for={[ColumnName.sha256]}
            formatterComponent={({ row }) => (
              <div
                className={getCeilClassname(row, 'sha256')}
                title={row.data.sha256}
              >
                {row.data.sha256}
              </div>
            )}
          />
          <DevExpessTable
            columnExtensions={tableColumnExtensions}
            cellComponent={props => {
              return <DevExpessTable.Cell {...props} />;
            }}
            rowComponent={Row}
          />
          <TableHeaderRow />
        </Grid>
      </Paper>
    </div>
  );
};

export default Table;
