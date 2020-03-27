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

import { IGitCodeBlob } from 'core/shared/models/Versioning/Blob/CodeBlob';
import { ComparedCommitType } from 'core/shared/models/Versioning/Blob/Diff';

interface ILocalProps {
  diffInfo: Record<keyof IGitCodeBlob['data'], boolean>;
  A?: IGitCodeBlob;
  B?: IGitCodeBlob;
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
  render(settings: IPropDefinitionRenderProps): React.ReactNode;
}
export interface IPropDefinitionRenderProps {
  type: ComparedCommitType;
  blobData?: IGitCodeBlob['data'];
  diffBlobProperties: Record<keyof IGitCodeBlob['data'], boolean>;
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
    const propDefinitions: IPropDefinition[] = this.getPropDefinitions();
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
          blobData: this.props.A ? this.props.A.data : undefined,
          diffBlobProperties: this.props.diffInfo,
        };
        return propDefinition.render(renderProps);
      }
      case ColumnNames.B: {
        const renderProps: IPropDefinitionRenderProps = {
          type: ColumnNames.B,
          blobData: this.props.B ? this.props.B.data : undefined,
          diffBlobProperties: this.props.diffInfo,
        };
        return propDefinition.render(renderProps);
      }
    }
  }
}

export { PropDefinition };
export default CompareTable;
