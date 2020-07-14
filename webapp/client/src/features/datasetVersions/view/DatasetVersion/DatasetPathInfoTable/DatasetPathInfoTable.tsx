import Paper from '@material-ui/core/Paper';
import { bind } from 'decko';
import * as React from 'react';

import { getFormattedDateTime } from 'shared/utils/formatters/dateTime';
import { formatBytes } from 'shared/utils/mapperConverters';
import removeQuotes from 'shared/utils/removeQuotes';
import Table from 'shared/view/elements/Table/Table';
import CopyButton from 'shared/view/elements/CopyButton/CopyButton';
import IdView from 'shared/view/elements/IdView/IdView';

import styles from './DatasetPathInfoTable.module.css';

interface IRow {
  path: string;
  size: string | number;
  checkSum: string;
  lastModified: Date;
  additionalClassname?: string | undefined;
}

interface ILocalProps {
  rows: IRow[];
}

const PathColumn = ({ row }: { row: IRow }) => {
  return (
    <span className={styles.table_content} title={row.path}>
      {row.path || '-'}
    </span>
  );
};

const SizeColumn = ({ row }: { row: IRow }) => {
  if (row.size) {
    const formatedSize = formatBytes(row.size);
    return (
      <span className={styles.table_content} title={formatedSize}>
        {formatedSize}
      </span>
    );
  }
  return <>-</>;
};

const ChecksumColumn = ({ row }: { row: IRow }) => {
  if (row.checkSum) {
    const formattedCheckSum = removeQuotes(row.checkSum);
    return (
      <span className={styles.table_content} title={formattedCheckSum}>
        <IdView value={formattedCheckSum} sliceStringUpto={7} />
        ... <CopyButton value={formattedCheckSum} />
      </span>
    );
  }
  return <>-</>;
};

const DateColumn = ({ row }: { row: IRow }) => {
  const dateModified = getFormattedDateTime(String(row.lastModified));
  if (row.lastModified) {
    return (
      <span className={styles.table_content} title={dateModified}>
        {dateModified}
      </span>
    );
  }
  return <>-</>;
};

export default class DatasetPathInfoTable extends React.PureComponent<
  ILocalProps
> {
  public render(): React.ReactNode {
    const { rows } = this.props;
    return (
      <Paper>
        <Table
          dataRows={rows}
          additionalClassNames={{
            root: styles.tableRoot,
          }}
          getRowKey={this.getRowKey}
          columnDefinitions={[
            {
              title: 'Path',
              type: 'path',
              width: '25%',
              render: row => <PathColumn row={row} />,
            },
            {
              title: 'Size',
              type: 'size',
              width: '25%',
              render: row => <SizeColumn row={row} />,
            },
            {
              title: 'CheckSum',
              type: 'checkSum',
              width: '25%',
              render: row => <ChecksumColumn row={row} />,
            },
            {
              title: 'Last Modified',
              type: 'lastModified',
              width: '25%',
              render: row => <DateColumn row={row} />,
            },
          ]}
        />
      </Paper>
    );
  }

  @bind
  private getRowKey(row: IRow) {
    return row.path;
  }
}
