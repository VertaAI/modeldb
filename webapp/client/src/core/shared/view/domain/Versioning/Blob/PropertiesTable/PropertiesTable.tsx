import { DataTypeProvider, Column } from '@devexpress/dx-react-grid';
import Paper from '@material-ui/core/Paper';
import { bind } from 'decko';
import * as R from 'ramda';
import * as React from 'react';

import {
  Table as TablePlugin,
  Grid,
  TableHeaderRow,
  TableWrapper,
} from 'core/shared/view/elements/Table/Plugins';

interface ILocalProps<T> {
  data: T;
  children: any;
}

const ColumnNames = {
  property: 'property',
  value: 'value',
};

interface IState {
  columns: Column[];
  tableColumnExtensions: any[];
}

export interface IPropDefinition<T> {
  title: string;
  render(settings: IPropDefinitionRenderProps<T>): React.ReactNode;
}
export interface IPropDefinitionRenderProps<T> {
  data: T;
}

function PropDefinition<T>(props: IPropDefinition<T>) {
  return null;
}

class Table<T> extends React.Component<ILocalProps<T>, IState> {
  constructor(props: ILocalProps<T>) {
    super(props);
    this.state = {
      columns: [
        {
          name: ColumnNames.property,
          title: 'Properties',
          getCellValue: R.identity,
        },
        {
          name: ColumnNames.value,
          title: ' ',
          getCellValue: R.identity,
        },
      ],
      tableColumnExtensions: [
        {
          columnName: ColumnNames.property,
          width: 190,
        },
      ],
    };
  }

  public render() {
    const { columns, tableColumnExtensions } = this.state;
    const propDefinitions: IPropDefinition<T>[] = this.getPropDefinitions();
    return (
      <Paper>
        <TableWrapper isHeightByContent={true}>
          <Grid rows={propDefinitions} columns={columns}>
            <DataTypeProvider
              formatterComponent={this.ColumnFactory as any}
              for={columns.map(({ name }) => name)}
            />
            <TablePlugin columnExtensions={tableColumnExtensions} />
            <TableHeaderRow />
          </Grid>
        </TableWrapper>
      </Paper>
    );
  }

  @bind
  private getPropDefinitions(): IPropDefinition<T>[] {
    return React.Children.map(
      this.props.children,
      (child: React.ReactElement) => child.props
    );
  }

  @bind
  private ColumnFactory({
    column,
    row: propDefinition,
  }: {
    column: { name: string };
    row: IPropDefinition<T>;
  }) {
    switch (column.name) {
      case ColumnNames.property: {
        return <span>{propDefinition.title}</span>;
      }
      case ColumnNames.value: {
        const renderProps: IPropDefinitionRenderProps<T> = {
          data: this.props.data,
        };
        return propDefinition.render(renderProps);
      }
    }
  }
}

export default function makePropertiesTableComponents<T>(): {
  Table: (props: ILocalProps<T>) => JSX.Element;
  PropDefinition: (props: IPropDefinition<T>) => null;
} {
  return { Table: Table as any, PropDefinition: PropDefinition as any };
}
