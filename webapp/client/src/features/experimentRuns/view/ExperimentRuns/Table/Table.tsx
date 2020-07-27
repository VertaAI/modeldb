import * as R from 'ramda';
import * as React from 'react';

import {
  IColumnConfig,
  IColumnMetaData,
} from 'features/experimentRunsTableConfig';
import { IPagination } from 'shared/models/Pagination';
import { ISorting } from 'shared/models/Sorting';
import withProps from 'shared/utils/react/withProps';
import { TableWrapper } from 'shared/view/elements/Table/Plugins';
import SelectFieldSorting from 'shared/view/elements/Table/Plugins/HeaderCell/SelectFieldSorting/SelectFieldSorting';
import PagingPanel from 'shared/view/elements/Table/Plugins/PagingPanel/TablePagingPanel';
import AppTable from 'shared/view/elements/Table/Table';
import { ColumnDefinition } from 'shared/view/elements/Table/types';
import ModelRecord from 'shared/models/ModelRecord';

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
  onResetShowingBulkDeletionMenu(): void;
}

const Table = ({
  columnConfig,
  data,
  pagination,
  sorting,
  projectId,
  withBulkDeletion,
  isShowBulkDeleteMenu,
  onResetShowingBulkDeletionMenu: resetShowingBulkDeletionMenu,
  onCurrentPageChange,
  onSortingChange,
}: ILocalProps) => {
  const [
    columnContentHeightById,
    updateColumnContentHeightById,
  ] = useColumnContentHeightById({ data });

  React.useEffect(() => {
    resetShowingBulkDeletionMenu();
  }, []);

  const hiddenColumnNames = Object.entries(columnConfig)
    .filter(([_, meta]: [any, IColumnMetaData]) => !meta.isShown)
    .map(([columnName]) => columnName);
  const columnDefinitions = useGetColumnDefinitions({
    data,
    sorting,
    onSortingChange,
    onUpdateColumnContentHeightById: updateColumnContentHeightById,
  });
  const displayedColumnDefinitions = (() => {
    return columnDefinitions.filter(c => !hiddenColumnNames.includes(c.type));
  })();
  const rows: IRow[] = data.map(experimentRun => ({
    experimentRun,
    columnContentHeight: columnContentHeightById[experimentRun.id] || 0,
  }));

  return (
    <TableWrapper>
      <AppTable
        dataRows={rows}
        columnDefinitions={displayedColumnDefinitions}
        getRowKey={getRowKey}
        selection={{
          headerCellComponent: () => <ToggleAllExperimentRunsForBulkDeletion />,
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
};

const useGetColumnDefinitions = ({
  data,
  sorting,
  onSortingChange,
  onUpdateColumnContentHeightById,
}: {
  data: ModelRecord[];
  sorting: ISorting | null;
  onSortingChange: (sorting: ISorting) => void;
  onUpdateColumnContentHeightById: (id: string, height: number) => void;
}): Array<ColumnDefinition<IRow>> => {
  const MemoSummarySorting = React.useMemo(() => {
    return withProps(SummarySorting)({
      onChange: onSortingChange,
      sorting,
    });
  }, [onSortingChange, sorting]);
  const MetricsSortingLabel = React.useMemo(() => {
    return withProps(KeyValueSortingLabel)({
      columnName: 'metrics',
      data,
      onChange: onSortingChange,
      sorting,
    });
  }, [data, onSortingChange, sorting]);
  const HyperparametersSortingLabel = React.useMemo(() => {
    return withProps(KeyValueSortingLabel)({
      columnName: 'hyperparameters',
      data,
      onChange: onSortingChange,
      sorting,
    });
  }, [data, onSortingChange, sorting]);

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
          onHeightChanged={onUpdateColumnContentHeightById}
        />
      ),
      width: '21%',
      withSort: true,
      customSortLabel: MemoSummarySorting,
    },
    {
      type: 'metrics',
      title: 'Metrics',
      render: row => <MetricsColumn row={row} />,
      width: '21%',
      withSort: true,
      customSortLabel: MetricsSortingLabel,
    },
    {
      type: 'hyperparameters',
      title: 'Hyperparameters',
      render: row => <HyperparametersColumn row={row} />,
      width: '21%',
      withSort: true,
      customSortLabel: HyperparametersSortingLabel,
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
};

const getRowKey = (row: IRow) => {
  return row.experimentRun.id;
};

const useColumnContentHeightById = ({ data }: { data: ModelRecord[] }) => {
  const [columnContentHeightById, setColumnContentHeightById] = React.useState<
    Record<string, number | undefined>
  >({});
  React.useEffect(() => {
    const initialColumnContentHeightById = R.fromPairs(
      R.zip(
        Array.from(
          document.querySelectorAll(
            '[data-column-name=experiment-runs-summary-column]'
          )
        ).map(
          summaryColumnContentElem =>
            (summaryColumnContentElem as HTMLElement)?.offsetHeight
        ),
        data
      ).map(([height, { id }]) => [id, height])
    );
    setColumnContentHeightById(initialColumnContentHeightById);
  }, []);

  const updateColumnContentHeightById = (id: string, height: number) => {
    if (columnContentHeightById[id] !== height) {
      setColumnContentHeightById({
        ...columnContentHeightById,
        [id]: height,
      });
    }
  };

  return [columnContentHeightById, updateColumnContentHeightById] as const;
};

const KeyValueSortingLabel = ({
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

const SummarySorting = ({
  sorting,
  onChange,
  children,
}: {
  sorting: ISorting | null;
  children: React.ReactNode;
  onChange(sorting: ISorting | null): void;
}) => {
  return (
    <>
      {children}
      <SelectFieldSorting
        fields={[{ label: 'Timestamp', name: 'date_created' }]}
        columnName={''}
        selected={sorting?.fieldName === 'date_created' ? sorting : null}
        onChange={onChange}
      />
    </>
  );
};

export default Table;
