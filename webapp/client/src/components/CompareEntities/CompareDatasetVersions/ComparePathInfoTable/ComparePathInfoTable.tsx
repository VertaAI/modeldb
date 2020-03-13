import { Column, DataTypeProvider } from '@devexpress/dx-react-grid';
import {
  Grid,
  Table,
  TableHeaderRow,
} from '@devexpress/dx-react-grid-material-ui';
import Paper from '@material-ui/core/Paper';
import * as R from 'ramda';
import * as React from 'react';

import { getDiffValueBgClassname } from 'components/CompareEntities/shared/DiffHighlight/DiffHighlight';
import { getFormattedDateTime } from 'core/shared/utils/formatters/dateTime';
import { formatBytes } from 'core/shared/utils/mapperConverters';
import removeQuotes from 'core/shared/utils/removeQuotes';
import CopyButton from 'core/shared/view/elements/CopyButton/CopyButton';
import IdView from 'core/shared/view/elements/IdView/IdView';
import { IDatasetPathPartInfo } from 'models/DatasetVersion';
import { IDiffDatasetPathInfos, EntityType } from 'store/compareEntities';

import styles from './ComparePathInfoTable.module.css';

interface IRow {
  data: IDatasetPathPartInfo;
  diffInfo: IDiffDatasetPathInfos[string];
  type: EntityType;
}

interface ILocalProps {
  diffInfo: IDiffDatasetPathInfos;
  pathInfos1: IDatasetPathPartInfo[];
  pathInfos2: IDatasetPathPartInfo[];
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

const Row = (props: { row: IRow; [key: string]: any }) => {
  const { children, row, tableRow, ...restProps } = props;
  const className = Object.values(row.diffInfo).every(v => v)
    ? getDiffValueBgClassname(row.type, true)
    : undefined;
  return (
    <tr {...restProps} className={className}>
      {children}
    </tr>
  );
};

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
  return '-';
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
  return '-';
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
  return '-';
};

export default class ComparePathInfoTable extends React.PureComponent<
  ILocalProps
> {
  public render(): React.ReactNode {
    const { diffInfo, pathInfos1, pathInfos2 } = this.props;

    const rows: IRow[] = R.sortBy(
      ({ data }) => data.path,
      (pathInfos1 || [])
        .map<[EntityType, IDatasetPathPartInfo]>(info => [
          EntityType.entity1,
          info,
        ])
        .concat(
          (pathInfos2 || []).map<[EntityType, IDatasetPathPartInfo]>(info => [
            EntityType.entity2,
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
            ? type === EntityType.entity1
            : true;
        })
    );

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
          <Table
            columnExtensions={tableColumnExtensions}
            cellComponent={props => {
              const additionalClassName = (props.row as IRow).diffInfo[
                props.column.name as keyof IRow['diffInfo']
              ]
                ? getDiffValueBgClassname(props.row.type, true)
                : undefined;
              return <Table.Cell {...props} className={additionalClassName} />;
            }}
            rowComponent={Row}
          />
          <TableHeaderRow />
        </Grid>
      </Paper>
    );
  }
}
