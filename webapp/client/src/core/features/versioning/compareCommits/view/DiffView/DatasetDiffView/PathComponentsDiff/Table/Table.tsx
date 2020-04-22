import { Column, DataTypeProvider } from '@devexpress/dx-react-grid';
import { Table as DevExpessTable } from '@devexpress/dx-react-grid-material-ui';
import Paper from '@material-ui/core/Paper';
import React from 'react';

import { IPathDatasetComponentBlob } from 'core/shared/models/Versioning/Blob/DatasetBlob';
import PathSize from 'core/shared/view/domain/Versioning/Blob/DatasetBlob/PathSize/PathSize';
import {
  Table as TablePlugin,
  Grid,
  TableHeaderRow,
  TableWrapper,
} from 'core/shared/view/elements/Table/Plugins';

import {
  IObjectToObjectWithDiffColor,
  getCssDiffColor,
} from '../../../../model';
import styles from './Table.module.css';
import withProps from 'core/shared/utils/react/withProps';
import { makeGenericCell } from 'core/shared/view/elements/Table/Templates/Cell/Cell';
import { TextWithCopyTooltip } from 'core/shared/view/elements/TextWithCopyTooltip/TextWithCopyTooltip';

interface ILocalProps {
  rows: IRow[];
}

export type IRow = IObjectToObjectWithDiffColor<IPathDatasetComponentBlob>;

const ColumnName: { [K in keyof IPathDatasetComponentBlob]: K } = {
  path: 'path',
  size: 'size',
  md5: 'md5',
  lastModifiedAtSource: 'lastModifiedAtSource',
  sha256: 'sha256',
};

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

const DiffCell = withProps(makeGenericCell<IRow, keyof typeof ColumnName>())({
  getStyle: (column, row) => {
    return row[column.name]
      ? { backgroundColor: getCssDiffColor(row[column.name].diffColor) }
      : undefined;
  },
  getDataType: column => column.name,
});

const Table: React.FC<ILocalProps> = ({ rows }) => {
  return (
    <div className={styles.root}>
      <Paper>
        <TableWrapper isHeightByContent={true}>
          <Grid rows={rows} columns={columns}>
            <ColumnProvider
              type={ColumnName.path}
              render={({ row }) => (
                <TextWithCopyTooltip copyText={row.path.value}>
                  <span title={row.path.value} data-test="path">
                    {row.path.value}
                  </span>
                </TextWithCopyTooltip>
              )}
            />
            <ColumnProvider
              type={ColumnName.size}
              render={({ row }) => <PathSize size={row.size.value} />}
            />
            <ColumnProvider
              type={ColumnName.md5}
              render={({ row }) => (
                <TextWithCopyTooltip
                  copyText={row.md5.value}
                  withEllipsis={true}
                >
                  <span title={row.md5.value} data-test="md5">
                    {row.md5.value}
                  </span>
                </TextWithCopyTooltip>
              )}
            />
            <ColumnProvider
              type={ColumnName.lastModifiedAtSource}
              render={({ row }) => (
                <div title={String(row.lastModifiedAtSource.value)}>
                  {row.lastModifiedAtSource.value.toLocaleDateString() +
                    ' ' +
                    row.lastModifiedAtSource.value.toLocaleTimeString()}
                </div>
              )}
            />
            <ColumnProvider
              type={ColumnName.sha256}
              render={({ row }) => (
                <TextWithCopyTooltip
                  copyText={row.sha256.value}
                  withEllipsis={true}
                >
                  <span data-test="sha256" title={row.sha256.value}>
                    {row.sha256.value}
                  </span>
                </TextWithCopyTooltip>
              )}
            />
            <TablePlugin
              columnExtensions={tableColumnExtensions}
              cellComponent={DiffCell}
            />
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
