import { Column, DataTypeProvider } from '@devexpress/dx-react-grid';
import { Table as DevExpessTable } from '@devexpress/dx-react-grid-material-ui';
import Paper from '@material-ui/core/Paper';
import cn from 'classnames';
import React from 'react';

import {
  Table as TablePlugin,
  Grid,
  TableHeaderRow,
  TableWrapper,
} from 'core/shared/view/elements/Table/Plugins';
import { IPathDatasetComponentBlob } from 'core/shared/models/Versioning/Blob/DatasetBlob';
import { formatBytes } from 'core/shared/utils/mapperConverters';

import styles from './Table.module.css';
import { IObjectToObjectWithDiffColor, DiffColor } from '../../../../model';

interface ILocalProps {
  rows: IRow[];
}

export type IRow = IObjectToObjectWithDiffColor<IPathDatasetComponentBlob>;

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
  { columnName: ColumnName.path, width: 250 },
  { columnName: ColumnName.size, width: 120 },
  { columnName: ColumnName.lastModifiedAtSource, width: 170 },
  { columnName: ColumnName.md5, width: 150 },
  { columnName: ColumnName.sha256 },
];

const getCeilClassname = (diffColor: DiffColor) => {
  return cn(styles.elem, {
    [styles.redDiff]: diffColor === 'red',
    [styles.greenDiff]: diffColor === 'green',
  });
};

const Table: React.FC<ILocalProps> = ({ rows }) => {
  return (
    <div className={styles.root}>
      <Paper>
        <TableWrapper isHeightByContent={true}>
          <Grid rows={rows} columns={columns}>
            <ColumnProvider
              type={ColumnName.path}
              render={({ row }) => (
                <div
                  className={getCeilClassname(row.path.diffColor)}
                  title={row.path.value}
                  data-test="path"
                >
                  {row.path.value}
                </div>
              )}
            />
            <ColumnProvider
              type={ColumnName.size}
              render={({ row }) => (
                <div
                  className={getCeilClassname(row.size.diffColor)}
                  title={String(row.size.value)}
                >
                  {formatBytes(row.size.value)}
                </div>
              )}
            />
            <ColumnProvider
              type={ColumnName.md5}
              render={({ row }) => (
                <div
                  className={getCeilClassname(row.md5.diffColor)}
                  title={row.md5.value}
                  data-test="md5"
                >
                  {row.md5.value}
                </div>
              )}
            />
            <ColumnProvider
              type={ColumnName.lastModifiedAtSource}
              render={({ row }) => (
                <div
                  className={getCeilClassname(
                    row.lastModifiedAtSource.diffColor
                  )}
                  title={String(row.lastModifiedAtSource.value)}
                >
                  {row.lastModifiedAtSource.value.toLocaleDateString() +
                    ' ' +
                    row.lastModifiedAtSource.value.toLocaleTimeString()}
                </div>
              )}
            />
            <ColumnProvider
              type={ColumnName.sha256}
              render={({ row }) => (
                <div
                  className={getCeilClassname(row.sha256.diffColor)}
                  data-test="sha256"
                  title={row.sha256.value}
                >
                  {row.sha256.value}
                </div>
              )}
            />
            <TablePlugin columnExtensions={tableColumnExtensions} />
            <TableHeaderRow />
          </Grid>
        </TableWrapper>
      </Paper>
    </div>
  );
};

const ColumnProvider = ({
  type,
  render,
}: {
  type: string;
  render: ({ row }: { row: IRow }) => JSX.Element;
}) => {
  return (
    <DataTypeProvider
      for={[type]}
      formatterComponent={({ row }) => render({ row: row as IRow })}
    />
  );
};

export default Table;
