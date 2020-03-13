import {
  DataTypeProvider,
  Column,
  SelectionState,
  IntegratedSelection,
} from '@devexpress/dx-react-grid';
import { TableSelection } from '@devexpress/dx-react-grid-material-ui';
import { bind } from 'decko';
import * as React from 'react';

import { IPagination } from 'core/shared/models/Pagination';
import {
  TableHeaderRow,
  Table as TablePlugin,
  Grid,
  TableWrapper,
} from 'core/shared/view/elements/Table/Plugins';
import PagingPanel from 'core/shared/view/elements/Table/Templates/PagingPanel/TablePagingPanel';
import { IDatasetVersion } from 'models/DatasetVersion';

import DeletingDatasetVersionsManager from './BulkDeletion/Manager/Manager';
import ToggleAllDatasetVersionsForBulkDeletion from './BulkDeletion/ToggleAllRows/ToggleAllRows';
import ToggleDatasetVersionForBulkDeletion from './BulkDeletion/ToggleRow/ToggleRow';
import styles from './DatasetVersionsTable.module.css';

interface ILocalProps {
  datasetId: string;
  data: IDatasetVersion[];
  withBulkDeletion: boolean;
  isShowBulkDeletionMenu: boolean;
  pagination: IPagination;
  children: Array<React.ReactElement<IDatasetVersionsTableColumnProps>>;
  onCurrentPageChange(currentPage: number): void;
  resetShowingBulkDeletionMenu(): void;
}

interface IDatasetVersionsTableColumnProps {
  type: string;
  title: string;
  width?: number;
  render(datasetVersion: IDatasetVersion): React.ReactNode;
}

const DatasetVersionsTableColumn = (
  props: IDatasetVersionsTableColumnProps
): any => {
  return props;
};

const noop = () => {};

class DatasetVersionsTable extends React.PureComponent<ILocalProps> {
  public static Column = DatasetVersionsTableColumn;

  public componentDidMount() {
    this.props.resetShowingBulkDeletionMenu();
  }
  public render() {
    const {
      data,
      pagination,
      withBulkDeletion,
      isShowBulkDeletionMenu,
      datasetId,
      onCurrentPageChange,
    } = this.props;
    const columns: Column[] = this.getColumnProps().map(columnProps => ({
      name: columnProps.type,
      title: columnProps.title,
      getCellValue: (x: any) => x,
    }));

    const providersElements = this.getColumnProps().map(({ type, render }) => (
      <DataTypeProvider
        key={type}
        formatterComponent={({ value }) => render(value) as any}
        for={[type]}
      />
    ));

    const tableColumnExtensions = this.getColumnProps()
      .filter(({ width }) => Boolean(width))
      .map(({ type, width }) => ({ columnName: type, width }));

    return (
      <TableWrapper>
        <Grid rows={data} columns={columns}>
          {providersElements}

          <SelectionState selection={[]} onSelectionChange={noop} />
          <IntegratedSelection />

          <TablePlugin columnExtensions={tableColumnExtensions} />
          <TableHeaderRow />

          {withBulkDeletion && isShowBulkDeletionMenu && (
            <TableSelection
              headerCellComponent={() => (
                <ToggleAllDatasetVersionsForBulkDeletion />
              )}
              cellComponent={props => {
                return (
                  <ToggleDatasetVersionForBulkDeletion id={props.row.id} />
                );
              }}
              showSelectAll={true}
              showSelectionColumn={true}
            />
          )}
        </Grid>
        <div className={styles.footer}>
          {withBulkDeletion && isShowBulkDeletionMenu && (
            <div>
              <DeletingDatasetVersionsManager datasetId={datasetId} />
            </div>
          )}
          <div className={styles.footer__pagination}>
            <PagingPanel
              pagination={pagination}
              onCurrentPageChange={onCurrentPageChange}
            />
          </div>
        </div>
      </TableWrapper>
    );
  }

  @bind
  private getColumnProps(): IDatasetVersionsTableColumnProps[] {
    return React.Children.map(
      this.props.children as any,
      (child: React.ReactElement) => child.props
    );
  }
}

export default DatasetVersionsTable;
