import Paper from '@material-ui/core/Paper';
import { bind } from 'decko';
import * as R from 'ramda';
import * as React from 'react';

import { getFormattedDateTime } from 'shared/utils/formatters/dateTime';
import { formatBytes } from 'shared/utils/mapperConverters';
import removeQuotes from 'shared/utils/removeQuotes';
import Table from 'shared/view/elements/Table/Table';
import CopyButton from 'shared/view/elements/CopyButton/CopyButton';
import IdView from 'shared/view/elements/IdView/IdView';
import { IDatasetPathPartInfo } from 'shared/models/DatasetVersion';
import { IDiffDatasetPathInfos, DatasetVerisonEntityType } from 'features/compareDatasets/store/compareDatasets';

import styles from './ComparePathInfoTable.module.css';
import { getDiffValueStyle } from '../../shared/DiffHighlight';

interface IRow {
  data: IDatasetPathPartInfo;
  diffInfo: IDiffDatasetPathInfos[string];
  type: DatasetVerisonEntityType;
}

interface ILocalProps {
  diffInfo: IDiffDatasetPathInfos;
  pathInfos1: IDatasetPathPartInfo[];
  pathInfos2: IDatasetPathPartInfo[];
}

const PathColumn = ({ row }: { row: IRow }) => {
  return (
    <span className={styles.table_content} title={row.data.path}>
      {row.data.path || '-'}
    </span>
  );
};

const SizeColumn = ({ row }: { row: IRow }) => {
  if (row.data.size) {
    const formatedSize = formatBytes(row.data.size);
    return (
      <span className={styles.table_content} title={formatedSize}>
        {formatedSize}
      </span>
    );
  }
  return <>'-'</>;
};

const ChecksumColumn = ({ row }: { row: IRow }) => {
  if (row.data.checkSum) {
    const formattedCheckSum = removeQuotes(row.data.checkSum);
    return (
      <span className={styles.table_content} title={formattedCheckSum}>
        <IdView value={formattedCheckSum} sliceStringUpto={7} />
        ... <CopyButton value={formattedCheckSum} />
      </span>
    );
  }
  return <>'-'</>;
};

const DateColumn = ({ row }: { row: IRow }) => {
  const dateModified = getFormattedDateTime(String(row.data.lastModified));
  if (row.data.lastModified) {
    return (
      <span className={styles.table_content} title={dateModified}>
        {dateModified}
      </span>
    );
  }
  return <>'-'</>;
};

export default class ComparePathInfoTable extends React.PureComponent<
  ILocalProps
> {
  public render(): React.ReactNode {
    const { diffInfo, pathInfos1, pathInfos2 } = this.props;

    const rows: IRow[] = R.sortBy(
      ({ data }) => data.path,
      (pathInfos1 || [])
        .map<[DatasetVerisonEntityType, IDatasetPathPartInfo]>(info => [
          DatasetVerisonEntityType.entity1,
          info,
        ])
        .concat(
          (pathInfos2 || []).map<[DatasetVerisonEntityType, IDatasetPathPartInfo]>(info => [
            DatasetVerisonEntityType.entity2,
            info,
          ])
        )
        .map(([entityType, pathInfo]) => ({
          data: pathInfo,
          diffInfo: diffInfo[pathInfo.path],
          type: entityType,
        }))
        .filter(({ data, diffInfo, type }) => {
          return Object.values(diffInfo).every(v => !v)
            ? type === DatasetVerisonEntityType.entity1
            : true;
        })
    );

    return (
      <Paper>
        <Table
          dataRows={rows}
          additionalClassNames={{ root: styles.tableRoot }}
          getRowKey={this.getRowKey}
          columnDefinitions={[
            {
              title: 'Path',
              type: 'path',
              width: '25%',
              render: row => <PathColumn row={row} />,
              getCellStyle: row =>
                getDiffValueStyle(row.type, row.diffInfo.path),
            },
            {
              title: 'Size',
              type: 'size',
              width: '25%',
              render: row => <SizeColumn row={row} />,
              getCellStyle: row =>
                getDiffValueStyle(row.type, row.diffInfo.size),
            },
            {
              title: 'CheckSum',
              type: 'checkSum',
              width: '25%',
              render: row => <ChecksumColumn row={row} />,
              getCellStyle: row =>
                getDiffValueStyle(row.type, row.diffInfo.checkSum),
            },
            {
              title: 'Last Modified',
              type: 'lastModified',
              width: '25%',
              render: row => <DateColumn row={row} />,
              getCellStyle: row =>
                getDiffValueStyle(row.type, row.diffInfo.lastModified),
            },
          ]}
        />
      </Paper>
    );
  }

  @bind
  private getRowKey(row: IRow) {
    return row.data.lastModified + row.type;
  }
}
