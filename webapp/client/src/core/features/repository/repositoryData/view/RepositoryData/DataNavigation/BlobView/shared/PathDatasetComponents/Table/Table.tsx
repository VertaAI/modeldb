import {
  DataTypeProvider,
  Column,
  IntegratedSorting,
  SortingState,
} from '@devexpress/dx-react-grid';
import { Paper } from '@material-ui/core';
import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';

import { IPathDatasetComponentBlob } from 'core/shared/models/Versioning/Blob/DatasetBlob';
import { Icon } from 'core/shared/view/elements/Icon/Icon';
import {
  TableHeaderRow,
  Table as TablePlugin,
  Grid,
  TableWrapper,
} from 'core/shared/view/elements/Table/Plugins';

import styles from './Table.module.css';

interface ILocalProps {
  data: IPathDatasetComponentBlob[];
  children: any;
}

interface ITableColumnProps {
  type: string;
  title: string;
  width?: number;
  render(data: IPathDatasetComponentBlob): React.ReactNode;
}

const ExperimentRunPropertyColumn = (props: ITableColumnProps): any => {
  return props;
};

const SortLabel = ({
  onSort,
  direction,
  children,
}: {
  onSort: () => void;
  direction?: string;
  children: React.ReactNode;
}) => {
  return (
    <div className={styles.sortLabel}>
      {children}
      <div className={styles.sortIcon} onClick={onSort}>
        <div className={styles.field_sorting_indicator}>
          <Icon
            type="arrow-up-lite"
            className={cn(styles.header_sorting_indicator_asc, {
              [styles.selected]: direction === 'asc',
            })}
          />
          <Icon
            type="arrow-down-lite"
            className={cn(styles.header_sorting_indicator_desc, {
              [styles.selected]: direction === 'desc',
            })}
          />
        </div>
      </div>
    </div>
  );
};

class Table extends React.PureComponent<ILocalProps> {
  public static Column = ExperimentRunPropertyColumn;

  public render() {
    const { data } = this.props;
    const columns: Column[] = this.getColumnProps().map(columnProps => ({
      name: columnProps.type,
      title: columnProps.title,
      getCellValue: (x: any) => x,
    }));

    const providersElements = this.getColumnProps().map(({ type, render }) => (
      <DataTypeProvider
        key={type}
        formatterComponent={({ value }) => <div>{render(value)}</div>}
        for={[type]}
      />
    ));

    const tableColumnExtensions = this.getColumnProps()
      .filter(({ width }) => Boolean(width))
      .map(({ type, width }) => ({ columnName: type, width }));

    return (
      <Paper>
        <TableWrapper isHeightByContent={true}>
          <Grid rows={data} columns={columns}>
            {providersElements}
            <SortingState />
            <IntegratedSorting />
            <TablePlugin columnExtensions={tableColumnExtensions} />
            <TableHeaderRow
              showSortingControls={true}
              sortLabelComponent={SortLabel as any}
            />
          </Grid>
        </TableWrapper>
      </Paper>
    );
  }

  @bind
  private getColumnProps(): ITableColumnProps[] {
    return React.Children.map(
      this.props.children as any,
      (child: React.ReactElement) => child.props
    );
  }
}

export default Table;
