import { DataTypeProvider, Column } from '@devexpress/dx-react-grid';
import { bind } from 'decko';
import * as React from 'react';

import { Paper } from '@material-ui/core';
import {
  TableHeaderRow,
  Table as TablePlugin,
  Grid,
  TableWrapper,
} from 'core/shared/view/elements/Table/Plugins';
import ModelRecord from 'models/ModelRecord';

interface ILocalProps {
  data: IRow[];
  children: Array<React.ReactElement<ITableColumnProps>>;
}

export type IRow = ModelRecord;

interface ITableColumnProps {
  type: string;
  title: string;
  width?: number;
  render(data: IRow): React.ReactNode;
}

const ExperimentRunPropertyColumn = (props: ITableColumnProps): any => {
  return props;
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
        <TableWrapper>
          <Grid rows={data} columns={columns}>
            {providersElements}

            <TablePlugin columnExtensions={tableColumnExtensions} />
            <TableHeaderRow />
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
