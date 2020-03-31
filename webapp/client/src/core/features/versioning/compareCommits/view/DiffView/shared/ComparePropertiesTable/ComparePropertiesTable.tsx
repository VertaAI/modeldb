import { DataTypeProvider, Column } from '@devexpress/dx-react-grid';
import Paper from '@material-ui/core/Paper';
import { bind } from 'decko';
import * as R from 'ramda';
import * as React from 'react';

import { ComparedCommitType } from 'core/shared/models/Versioning/Blob/Diff';
import {
  Table as TablePlugin,
  Grid,
  TableHeaderRow,
  TableWrapper,
} from 'core/shared/view/elements/Table/Plugins';

import { IComparedCommitsInfo } from '../../../model';
import { getColumnComparedCommitsTitles } from '../comparedCommitsNames';

interface ILocalProps<T> {
  comparedCommitsInfo: IComparedCommitsInfo;
  A?: T;
  B?: T;
  children: any;
}

const ColumnNames: { [K in ComparedCommitType]: K } & {
  properties: 'properties';
} = {
  A: 'A',
  B: 'B',
  properties: 'properties',
};

interface IState {
  columns: Column[];
  tableColumnExtensions: any[];
}

export interface IPropDefinition<T> {
  title: string;
  isHidden?: boolean;
  render(settings: IPropDefinitionRenderProps<T>): React.ReactNode;
}
export interface IPropDefinitionRenderProps<T> {
  type: ComparedCommitType;
  data?: T;
  anotherData?: T;
}

function PropDefinition<T>(props: IPropDefinition<T>) {
  return null;
}

class ComparePropertiesTable<T> extends React.Component<
  ILocalProps<T>,
  IState
> {
  public static PropDefinition = PropDefinition;

  constructor(props: ILocalProps<T>) {
    super(props);
    const columnsComparedCommitsTitles = getColumnComparedCommitsTitles(
      props.comparedCommitsInfo
    );
    this.state = {
      columns: [
        {
          name: ColumnNames.properties,
          title: 'Properties',
          getCellValue: R.identity,
        },
        {
          name: ColumnNames.A,
          title: columnsComparedCommitsTitles.A.title,
          getCellValue: R.identity,
        },
        {
          name: ColumnNames.B,
          title: columnsComparedCommitsTitles.B.title,
          getCellValue: R.identity,
        },
      ],
      tableColumnExtensions: [
        {
          columnName: ColumnNames.properties,
          width: 190,
        },
      ],
    };
  }

  public render() {
    const { columns, tableColumnExtensions } = this.state;
    const propDefinitions: IPropDefinition<
      T
    >[] = this.getPropDefinitions().filter(
      ({ isHidden = false }) => isHidden !== true
    );
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
      case ColumnNames.properties: {
        return <span>{propDefinition.title}</span>;
      }
      case ColumnNames.A: {
        const renderProps: IPropDefinitionRenderProps<T> = {
          type: ColumnNames.A,
          data: this.props.A ? this.props.A : undefined,
          anotherData: this.props.B ? this.props.B : undefined,
        };
        return propDefinition.render(renderProps);
      }
      case ColumnNames.B: {
        const renderProps: IPropDefinitionRenderProps<T> = {
          type: ColumnNames.B,
          data: this.props.B ? this.props.B : undefined,
          anotherData: this.props.A ? this.props.A : undefined,
        };
        return propDefinition.render(renderProps);
      }
    }
  }
}

export default function makeComparePropertiesTable<T>(): {
  Table: (props: ILocalProps<T>) => JSX.Element;
  PropDefinition: (props: IPropDefinition<T>) => null;
} {
  return {
    Table: ComparePropertiesTable as any,
    PropDefinition: PropDefinition as any,
  };
}
