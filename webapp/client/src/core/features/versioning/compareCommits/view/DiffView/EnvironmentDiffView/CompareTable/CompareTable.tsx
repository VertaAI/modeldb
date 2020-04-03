import { DataTypeProvider, Column } from '@devexpress/dx-react-grid';
import Paper from '@material-ui/core/Paper';
import { bind } from 'decko';
import * as R from 'ramda';
import * as React from 'react';

import { ComparedCommitType } from 'core/shared/models/Versioning/Blob/Diff';
import { IEnvironmentBlobDataDiff } from 'core/shared/models/Versioning/Blob/EnvironmentBlob';
import {
  Table as TablePlugin,
  Grid,
  TableHeaderRow,
  TableWrapper,
} from 'core/shared/view/elements/Table/Plugins';

import { BlobDataBox } from 'core/shared/view/domain/Versioning/Blob/BlobBox/BlobBox';

interface ILocalProps {
  title: string;
  A?: IEnvironmentBlobDataDiff;
  B?: IEnvironmentBlobDataDiff;
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
  tableColumnExtensions: any[];
}

export interface IPropDefinition {
  title: string;
  isHidden?: boolean;
  render(settings: IPropDefinitionRenderProps): React.ReactNode;
}
export interface IPropDefinitionRenderProps {
  type: ComparedCommitType;
  blobData?: IEnvironmentBlobDataDiff;
  anotherBlobData?: IEnvironmentBlobDataDiff;
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
    const { title } = this.props;
    const { columns, tableColumnExtensions } = this.state;
    const propDefinitions: IPropDefinition[] = this.getPropDefinitions().filter(
      ({ isHidden }) => isHidden !== true
    );

    return (
      <BlobDataBox title={title}>
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
      </BlobDataBox>
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
          blobData: this.props.A,
          anotherBlobData: this.props.B,
        };
        return propDefinition.render(renderProps);
      }
      case ColumnNames.B: {
        const renderProps: IPropDefinitionRenderProps = {
          type: ColumnNames.B,
          blobData: this.props.B,
          anotherBlobData: this.props.A,
        };
        return propDefinition.render(renderProps);
      }
    }
  }
}

export { PropDefinition };
export default CompareTable;
