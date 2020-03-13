import { Column, DataTypeProvider } from '@devexpress/dx-react-grid';
import {
  Grid,
  Table,
  TableHeaderRow,
} from '@devexpress/dx-react-grid-material-ui';
import Paper from '@material-ui/core/Paper';
import * as React from 'react';

import { getFormattedDateTime } from 'core/shared/utils/formatters/dateTime';
import { formatBytes } from 'core/shared/utils/mapperConverters';
import removeQuotes from 'core/shared/utils/removeQuotes';

import CopyButton from 'core/shared/view/elements/CopyButton/CopyButton';
import IdView from 'core/shared/view/elements/IdView/IdView';
import styles from './DatasetPathInfoTable.module.css';

interface IRow {
  path: string;
  size: string | number;
  checkSum: string;
  additionalClassname?: string | undefined;
}

interface ILocalProps {
  rows: IRow[];
}

enum ColumnName {
  path = 'path',
  size = 'size',
  checkSum = 'checkSum',
  lastModified = 'lastModified',
}

const columns: Column[] = [
  { name: ColumnName.path, title: 'Path' },
  { name: ColumnName.size, title: 'Size' },
  { name: ColumnName.checkSum, title: 'CheckSum' },
  { name: ColumnName.lastModified, title: 'Last Modified' },
];
const tableColumnExtensions: Table.ColumnExtension[] = [
  { columnName: ColumnName.path, width: 200, align: 'center' },
  { columnName: ColumnName.size, width: 80, align: 'center' },
  { columnName: ColumnName.checkSum, width: 160, align: 'center' },
  { columnName: ColumnName.lastModified, width: 240, align: 'center' },
];

const Root = (props: any) => <Grid.Root {...props} style={{ width: '100%' }} />;

const Row = (props: any) => {
  const { children, row, tableRow, ...restProps } = props;
  return (
    <tr {...restProps} className={row.additionalClassname}>
      {children}
    </tr>
  );
};

const PathColumn = ({
  row,
}: DataTypeProvider.ValueFormatterProps & { row: IRow }) => {
  return (
    <span className={styles.table_content} title={row.path}>
      {row.path || '-'}
    </span>
  );
};

const SizeColumn = ({
  row,
}: DataTypeProvider.ValueFormatterProps & { row: IRow }) => {
  if (row.size) {
    const formatedSize = formatBytes(row.size);
    return (
      <span className={styles.table_content} title={formatedSize}>
        {formatedSize}
      </span>
    );
  }
  return '-';
};

const ChecksumColumn = ({
  row,
}: DataTypeProvider.ValueFormatterProps & { row: IRow }) => {
  if (row.checkSum) {
    const formattedCheckSum = removeQuotes(row.checkSum);
    return (
      <span className={styles.table_content} title={formattedCheckSum}>
        <IdView value={formattedCheckSum} sliceStringUpto={7} />
        ... <CopyButton value={formattedCheckSum} />
      </span>
    );
  }
  return '-';
};

const DateColumn = ({
  row,
}: DataTypeProvider.ValueFormatterProps & { row: IRow }) => {
  const dateModified = getFormattedDateTime(String(row.lastModified));
  if (row.lastModified) {
    return (
      <span className={styles.table_content} title={dateModified}>
        {dateModified}
      </span>
    );
  }
  return '-';
};

export default class DatasetPathInfoTable extends React.PureComponent<
  ILocalProps
> {
  public render(): React.ReactNode {
    const { rows } = this.props;
    return (
      <Paper>
        <Grid rows={rows} columns={columns} rootComponent={Root}>
          <DataTypeProvider
            for={[ColumnName.path]}
            formatterComponent={PathColumn as any}
          />
          <DataTypeProvider
            for={[ColumnName.size]}
            formatterComponent={SizeColumn as any}
          />
          <DataTypeProvider
            for={[ColumnName.checkSum]}
            formatterComponent={ChecksumColumn as any}
          />
          <DataTypeProvider
            for={[ColumnName.lastModified]}
            formatterComponent={DateColumn as any}
          />
          <Table columnExtensions={tableColumnExtensions} rowComponent={Row} />
          <TableHeaderRow />
        </Grid>
      </Paper>
    );
  }
}
