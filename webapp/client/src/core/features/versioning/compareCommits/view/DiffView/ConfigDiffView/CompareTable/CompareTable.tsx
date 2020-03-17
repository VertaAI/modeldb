import { DataTypeProvider, Column } from '@devexpress/dx-react-grid';
import {
  Table,
  Grid,
  TableHeaderRow,
} from '@devexpress/dx-react-grid-material-ui';
import Paper from '@material-ui/core/Paper';
import { bind } from 'decko';
import * as R from 'ramda';
import * as React from 'react';

import { IConfigBlobDataDiff } from 'core/shared/models/Versioning/Blob/ConfigBlob';
import { ComparedCommitType } from 'core/shared/models/Versioning/Blob/Diff';

interface ILocalProps {
  blobA?: IConfigBlobDataDiff['data'];
  blobB?: IConfigBlobDataDiff['data'];
  columns: Record<ComparedCommitType, { title: string }> & {
    property: { title: string; width: number };
  };
  children: any;
}

const ColumnNames: { [T in keyof ILocalProps['columns']]: T } = {
  A: 'A',
  B: 'B',
  property: 'property',
};

interface IState {
  columns: Column[];
  tableColumnExtensions: Table.ColumnExtension[];
}

export interface IPropDefinition {
  title: string;
  isHidden?: boolean;
  render(settings: IPropDefinitionRenderProps): React.ReactNode;
}
export interface IPropDefinitionRenderProps {
  type: ComparedCommitType;
  blobData?: IConfigBlobDataDiff['data'];
  anotherBlobData?: IConfigBlobDataDiff['data'];
}

function PropDefinition(props: IPropDefinition) {
  return null;
}

class CompareTable extends React.Component<ILocalProps, IState> {
  public static PropDefinition = PropDefinition;

  constructor(props: ILocalProps) {
    super(props);
    const columnsSettings = this.props.columns;
    this.state = {
      columns: [
        {
          name: ColumnNames.property,
          title: columnsSettings.property.title,
          getCellValue: R.identity,
        },
        {
          name: ColumnNames.A,
          title: columnsSettings.A.title,
          getCellValue: R.identity,
        },
        {
          name: ColumnNames.B,
          title: columnsSettings.B.title,
          getCellValue: R.identity,
        },
      ],
      tableColumnExtensions: [
        {
          columnName: ColumnNames.property,
          width: columnsSettings.property.width,
        },
      ],
    };
  }

  public render() {
    const { columns, tableColumnExtensions } = this.state;
    const propDefinitions: IPropDefinition[] = this.getPropDefinitions().filter(
      ({ isHidden }) => isHidden !== true
    );
    return (
      <Paper>
        <Grid rows={propDefinitions} columns={columns}>
          <DataTypeProvider
            formatterComponent={this.ColumnFactory as any}
            for={columns.map(({ name }) => name)}
          />
          <Table columnExtensions={tableColumnExtensions} />
          <TableHeaderRow />
        </Grid>
      </Paper>
    );
  }

  @bind
  private getPropDefinitions(): IPropDefinition[] {
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
    row: IPropDefinition;
  }) {
    switch (column.name) {
      case ColumnNames.property: {
        return <span>{propDefinition.title}</span>;
      }
      case ColumnNames.A: {
        const renderProps: IPropDefinitionRenderProps = {
          type: ColumnNames.A,
          blobData: this.props.blobA,
          anotherBlobData: this.props.blobB,
        };
        return propDefinition.render(renderProps);
      }
      case ColumnNames.B: {
        const renderProps: IPropDefinitionRenderProps = {
          type: ColumnNames.B,
          blobData: this.props.blobB,
          anotherBlobData: this.props.blobA,
        };
        return propDefinition.render(renderProps);
      }
    }
  }
}

export { PropDefinition };
export default CompareTable;
