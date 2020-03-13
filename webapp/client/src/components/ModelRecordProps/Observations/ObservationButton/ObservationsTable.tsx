import { Column } from '@devexpress/dx-react-grid';
import {
  Grid,
  Table,
  TableHeaderRow,
} from '@devexpress/dx-react-grid-material-ui';
import Paper from '@material-ui/core/Paper';
import * as React from 'react';

interface IRow {
  timeStamp: string;
  value: string | number;
}

interface ILocalProps {
  rows: IRow[];
}

const columns: Column[] = [
  { name: 'timeStamp', title: 'TimeStamp' },
  { name: 'value', title: 'Value' },
];
const tableColumnExtensions: Table.ColumnExtension[] = [
  { columnName: 'timeStamp', width: 220, align: 'center' },
  { columnName: 'value', width: 220, align: 'center' },
];

export default class ObservationsTable extends React.PureComponent<
  ILocalProps
> {
  public render(): React.ReactNode {
    const { rows } = this.props;
    return (
      <Paper>
        <Grid rows={rows} columns={columns}>
          <Table columnExtensions={tableColumnExtensions} />
          <TableHeaderRow />
        </Grid>
      </Paper>
    );
  }
}
