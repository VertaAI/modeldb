import { bind } from 'decko';
import * as R from 'ramda';
import * as React from 'react';

import {
  IColumnConfig,
  IColumnMetaData,
} from 'core/features/experimentRunsTableConfig';
import { IPagination } from 'core/shared/models/Pagination';
import { ISorting } from 'core/shared/models/Sorting';
import withProps from 'core/shared/utils/react/withProps';
import { TableWrapper } from 'core/shared/view/elements/Table/Plugins';
import SelectFieldSorting from 'core/shared/view/elements/Table/Plugins/HeaderCell/SelectFieldSorting/SelectFieldSorting';
import PagingPanel from 'core/shared/view/elements/Table/Plugins/PagingPanel/TablePagingPanel';
import AppTable from 'core/shared/view/elements/Table/Table';
import { ColumnDefinition } from 'core/shared/view/elements/Table/types';
import ModelRecord from 'core/shared/models/ModelRecord';

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
  columnContentHeightById: Record<string, number | undefined>;
}

class Table extends React.Component<ILocalProps, ILocalState> {
  public state: ILocalState = {
    columnContentHeightById: {},
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

    const hiddenColumnNames = Object.entries(columnConfig)
      .filter(([_, meta]: [any, IColumnMetaData]) => !meta.isShown)
      .map(([columnName]) => columnName);

    const columnDefinitions = this.getColumnDefinitions({
      data,
      sorting,
      onChange: onSortingChange,
    });

    const filteredColumnDefinitions = columnDefinitions.filter(
      c => !hiddenColumnNames.includes(c.type)
    );

    const rows: IRow[] = data.map(experimentRun => ({
      experimentRun,
      columnContentHeight:
        this.state.columnContentHeightById[experimentRun.id] || 0,
    }));

    return (
      <TableWrapper>
        <AppTable
          dataRows={rows}
          columnDefinitions={filteredColumnDefinitions}
          getRowKey={this.getRowKey}
          selection={{
            headerCellComponent: () => (
              <ToggleAllExperimentRunsForBulkDeletion />
            ),
            cellComponent: row => {
              return (
                <ToggleExperimentRunForBulkDeletion id={row.experimentRun.id} />
              );
            },
            showSelectAll: withBulkDeletion && isShowBulkDeleteMenu,
            showSelectionColumn: withBulkDeletion && isShowBulkDeleteMenu,
          }}
        />

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
  private getRowKey(row: IRow) {
    return row.experimentRun.id;
  }

  @bind
  private getColumnDefinitions({
    data,
    onChange,
    sorting,
  }: {
    data: ModelRecord[];
    sorting: ISorting | null;
    onChange: (sorting: ISorting) => void;
  }): Array<ColumnDefinition<IRow>> {
    return [
      {
        type: 'actions',
        title: 'Actions',
        render: row => <ActionsColumn row={row} />,
        width: '16%',
      },
      {
        type: 'summary',
        title: 'Run Summary',
        render: row => (
          <SummaryColumn
            row={row}
            onHeightChanged={this.updateColumnContentHeightById}
          />
        ),
        width: '21%',
      },
      {
        type: 'metrics',
        title: 'Metrics',
        render: row => <MetricsColumn row={row} />,
        width: '21%',
        withSort: true,
        getValue: () => 1,
        customSortLabel: withProps(SortingLabel)({
          columnName: 'metrics',
          data,
          onChange,
          sorting,
        }),
      },
      {
        type: 'hyperparameters',
        title: 'Hyperparameters',
        render: row => <HyperparametersColumn row={row} />,
        width: '21%',
        withSort: true,
        getValue: () => 1,
        customSortLabel: withProps(SortingLabel)({
          columnName: 'hyperparameters',
          data,
          onChange,
          sorting,
        }),
      },
      {
        type: 'artifacts',
        title: 'Artifacts',
        render: row => <ArtifactsColumn row={row} />,
        width: '21%',
      },
      {
        type: 'observations',
        title: 'Observations',
        render: row => <ObservationsColumn row={row} />,
        width: '20%',
      },
      {
        type: 'attributes',
        title: 'Attributes',
        render: row => <AttributesColumn row={row} />,
        width: '20%',
      },
      {
        type: 'codeVersion',
        title: 'Code Version',
        render: row => <CodeVersionColumn row={row} />,
        width: '20%',
      },
      {
        type: 'datasets',
        title: 'Datasets',
        render: row => <DatasetsColumn row={row} />,
        width: '20%',
      },
    ];
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

const SortingLabel = ({
  data,
  columnName,
  sorting,
  onChange,
  children,
}: {
  data: ModelRecord[];
  columnName: 'metrics' | 'hyperparameters';
  sorting: ISorting | null;
  children: React.ReactNode;
  onChange(sorting: ISorting | null): void;
}) => {
  const sortingKeys = R.uniq(
    data.map(expRun => expRun[columnName].map(p => p.key)).flat()
  );

  if (sorting && !sortingKeys.includes(sorting.fieldName)) {
    sortingKeys.push(sorting.fieldName);
  }

  const sortingFields = sortingKeys.map(k => ({
    label: k,
    name: k,
  }));

  const isSelected = sorting && sorting.columnName === columnName;

  return (
    <>
      {children}
      <SelectFieldSorting
        fields={sortingFields}
        columnName={columnName}
        selected={isSelected ? sorting : null}
        onChange={onChange}
      />
    </>
  );
};

export default Table;
