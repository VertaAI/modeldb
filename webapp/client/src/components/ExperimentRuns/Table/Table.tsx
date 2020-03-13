import {
  SelectionState,
  IntegratedSelection,
  Column,
  GridColumnExtension,
} from '@devexpress/dx-react-grid';
import { TableSelection } from '@devexpress/dx-react-grid-material-ui';
import { bind } from 'decko';
import * as R from 'ramda';
import * as React from 'react';

import {
  IColumnConfig,
  IColumnMetaData,
} from 'core/features/experimentRunsTableConfig';
import { IPagination } from 'core/shared/models/Pagination';
import { ISorting } from 'core/shared/models/Sorting';
import {
  ColumnFieldSorting,
  Grid,
  Table as TablePlugin,
  TableColumnVisibility,
  TableHeaderRow,
  TableWrapper,
} from 'core/shared/view/elements/Table/Plugins';
import RowSelection from 'core/shared/view/elements/Table/Plugins/RowSelection';
import PagingPanel from 'core/shared/view/elements/Table/Templates/PagingPanel/TablePagingPanel';
import ModelRecord from 'models/ModelRecord';

import DeletingExperimentRunsManager from './BulkDeletion/DeletingExperimentRunsManager/DeletingExperimentRunsManager';
import ToggleAllExperimentRunsForBulkDeletion from './BulkDeletion/ToggleAllExperimentRunsForBulkDeletion/ToggleAllExperimentRunsForBulkDeletion';
import ToggleExperimentRunForBulkDeletion from './BulkDeletion/ToggleExperimentRunForBulkDeletion/ToggleExperimentRunForBulkDeletion';
import {
  ActionsColumn,
  ArtifactsColumn,
  AttributesColumn,
  CodeVersionColumn,
  DatasetsColumn,
  HyperparametersColumn,
  MetricsColumn,
  ObservationsColumn,
  SummaryColumn,
} from './ColumnDefinitions';
import { IRow } from './ColumnDefinitions/types';
import styles from './Table.module.css';

interface ILocalProps {
  projectId: string;
  withBulkDeletion: boolean;
  data: ModelRecord[];
  columnConfig: IColumnConfig;
  pagination: IPagination;
  sorting: ISorting | null;
  isShowBulkDeleteMenu: boolean;
  onCurrentPageChange(currentPage: number): void;
  onSortingChange(sorting: ISorting | null): void;
  resetShowingBulkDeletionMenu(): void;
}

interface ILocalState {
  columns: Column[];
  tableColumnExtensions: GridColumnExtension[];
  columnContentHeightById: Record<string, number | undefined>;
}

const noop = () => {};

class Table extends React.Component<ILocalProps, ILocalState> {
  public state: ILocalState = {
    columnContentHeightById: {},
    columns: [
      { name: 'actions', title: 'Actions' },
      { name: 'summary', title: 'Run Summary' },
      { name: 'metrics', title: 'Metrics' },
      { name: 'hyperparameters', title: 'Hyperparameters' },
      { name: 'artifacts', title: 'Artifacts' },
      { name: 'observations', title: 'Observations' },
      { name: 'attributes', title: 'Attributes' },
      { name: 'codeVersion', title: 'Code Version' },
      { name: 'datasets', title: 'Datasets' },
    ],
    tableColumnExtensions: [
      { columnName: 'actions', width: 140 },
      { columnName: 'summary', width: 240 },
      { columnName: 'metrics', width: 200 },
      { columnName: 'artifacts', width: 200 },
      { columnName: 'hyperparameters', width: 210 },
      { columnName: 'attributes', width: 200 },
      { columnName: 'observations', width: 200 },
      { columnName: 'codeVersion', width: 210 },
      { columnName: 'datasets', width: 200 },
    ],
  };

  public componentDidMount() {
    this.props.resetShowingBulkDeletionMenu();

    this.setState({
      columnContentHeightById: this.getColumnContentHeightById(),
    });
  }

  public render() {
    const {
      columnConfig,
      data,
      pagination,
      sorting,
      projectId,
      withBulkDeletion,
      isShowBulkDeleteMenu,
      onCurrentPageChange,
      onSortingChange,
    } = this.props;
    const { columns, tableColumnExtensions } = this.state;

    const hiddenColumnNames = Object.entries(columnConfig)
      .filter(([_, meta]: [any, IColumnMetaData]) => !meta.isShown)
      .map(([columnName]) => columnName);

    const rows: IRow[] = data.map(experimentRun => ({
      experimentRun,
      columnContentHeight:
        this.state.columnContentHeightById[experimentRun.id] || 0,
    }));

    return (
      <TableWrapper>
        <Grid rows={rows} columns={columns}>
          <ColumnFieldSorting<IRow, 'metrics' | 'hyperparameters'>
            columnNames={['metrics', 'hyperparameters']}
            getFieldNames={({ experimentRun }, columnName) =>
              experimentRun[columnName].map(({ key }) => key)
            }
            sorting={sorting}
            onSortingChange={onSortingChange}
          />

          <RowSelection getBodyRow={noop} getHeaderRow={noop} />

          <ActionsColumn for={['actions']} />
          <SummaryColumn
            for={['summary']}
            onHeightChanged={this.updateColumnContentHeightById}
          />
          <MetricsColumn for={['metrics']} />
          <ArtifactsColumn for={['artifacts']} />
          <HyperparametersColumn for={['hyperparameters']} />
          <AttributesColumn for={['attributes']} />
          <ObservationsColumn for={['observations']} />
          <CodeVersionColumn for={['codeVersion']} />
          <DatasetsColumn for={['datasets']} />

          <SelectionState selection={[]} onSelectionChange={noop} />
          <IntegratedSelection />

          <TablePlugin columnExtensions={tableColumnExtensions} />
          <TableHeaderRow />

          {withBulkDeletion && isShowBulkDeleteMenu && (
            <TableSelection
              headerCellComponent={() => (
                <ToggleAllExperimentRunsForBulkDeletion />
              )}
              cellComponent={props => {
                return (
                  <ToggleExperimentRunForBulkDeletion
                    id={props.row.experimentRun.id}
                  />
                );
              }}
              showSelectAll={true}
              showSelectionColumn={true}
            />
          )}

          <TableColumnVisibility hiddenColumnNames={hiddenColumnNames} />
        </Grid>
        <div className={styles.footer}>
          {withBulkDeletion && isShowBulkDeleteMenu && (
            <div className={styles.footer__bulkDeletionManager}>
              <DeletingExperimentRunsManager projectId={projectId} />
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
  private getColumnContentHeightById(): ILocalState['columnContentHeightById'] {
    return R.fromPairs(R.zip(
      Array.from(
        document.querySelectorAll(
          '[data-column-name=experiment-runs-summary-column]'
        )
      ).map(
        summaryColumnContentElem =>
          (summaryColumnContentElem as HTMLElement).offsetHeight
      ),
      this.props.data
    ).map(([height, { id }]) => [id, height]) as any);
  }

  @bind
  private updateColumnContentHeightById(id: string, height: number) {
    if (this.state.columnContentHeightById[id] !== height) {
      this.setState(prev => ({
        columnContentHeightById: {
          ...prev.columnContentHeightById,
          [id]: height,
        },
      }));
    }
  }
}

export default Table;
