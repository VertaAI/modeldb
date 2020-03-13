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

import { IDatasetVersion } from 'models/DatasetVersion';
import {
  IDatasetVersionsDifferentProps,
  ComparedDatasetVersions,
  EntityType,
} from 'store/compareEntities';

interface ILocalProps {
  datasetVersionsDifferentProps: IDatasetVersionsDifferentProps;
  datasetVersions: Required<ComparedDatasetVersions>;
  columns: {
    property: {
      title: string;
      width: number;
    };
    [EntityType.entity1]: {
      title: string;
    };
    [EntityType.entity2]: {
      title: string;
    };
  };
  children: Array<React.ReactElement<IPropDefinition>>;
}

interface IState {
  columns: Column[];
  tableColumnExtensions: Table.ColumnExtension[];
}

export interface IPropDefinition<
  Prop extends keyof IDatasetVersionsDifferentProps = keyof IDatasetVersionsDifferentProps,
  DatasetVersion extends IDatasetVersion = IDatasetVersion
> {
  prop: Prop;
  title: string;
  getValue(datasetVersion: DatasetVersion): any;
  render(settings: IPropDefinitionRenderProps<Prop>): React.ReactNode;
}
export interface IPropDefinitionRenderProps<
  Prop extends keyof IDatasetVersionsDifferentProps = keyof IDatasetVersionsDifferentProps,
  DatasetVersion extends IDatasetVersion = IDatasetVersion
> {
  value: any;
  enitityType: EntityType;
  prop: Prop;
  datasetVersion: DatasetVersion;
  diffInfo: Prop extends keyof IDatasetVersionsDifferentProps
    ? IDatasetVersionsDifferentProps[Prop]
    : undefined;
}

function PropDefinition<
  DatasetVersion extends IDatasetVersion = IDatasetVersion
>(props: IPropDefinition<any, DatasetVersion>) {
  return null;
}

class CompareDatasetVersionsTable extends React.Component<ILocalProps, IState> {
  constructor(props: ILocalProps) {
    super(props);
    const columnsSettings = this.props.columns;
    this.state = {
      columns: [
        {
          name: 'propertyName',
          title: columnsSettings.property.title,
          getCellValue: R.identity,
        },
        {
          name: EntityType.entity1,
          title: columnsSettings[EntityType.entity1].title,
          getCellValue: R.identity,
        },
        {
          name: EntityType.entity2,
          title: columnsSettings[EntityType.entity2].title,
          getCellValue: R.identity,
        },
      ],
      tableColumnExtensions: [
        { columnName: 'propertyName', width: columnsSettings.property.width },
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
          <Table
            columnExtensions={tableColumnExtensions}
            cellComponent={props => {
              const additionalProps = (() => {
                if (props.row.prop === 'datasetPathInfos') {
                  if (props.column.name === 'entity1') {
                    return { colSpan: 2 };
                  }
                  if (props.column.name === 'entity2') {
                    return { style: { display: 'none' } };
                  }
                }
                return {};
              })();
              return <Table.Cell {...props} {...additionalProps} />;
            }}
          />
          <TableHeaderRow />
        </Grid>
      </Paper>
    );
  }

  @bind
  private getPropDefinitions(): IPropDefinition[] {
    return R.chain(
      (child: any) =>
        child.type === React.Fragment ? child.props.children : child,
      this.props.children
    ).map((child: any) => child.props);
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
      case 'propertyName': {
        return (
          <span data-test={`property-name-${propDefinition.prop}`}>
            {propDefinition.title}
          </span>
        );
      }
      case EntityType.entity1: {
        const renderProps = this.getDatasetVersionPropRenderProps(
          EntityType.entity1,
          this.props.datasetVersions[0],
          propDefinition
        );
        return propDefinition.render(renderProps);
      }
      case EntityType.entity2: {
        const renderProps = this.getDatasetVersionPropRenderProps(
          EntityType.entity2,
          this.props.datasetVersions[1],
          propDefinition
        );
        return propDefinition.render(renderProps);
      }
    }
  }

  @bind
  private getDatasetVersionPropRenderProps(
    entityType: EntityType,
    datasetVersion: IDatasetVersion,
    propDefinition: IPropDefinition
  ): IPropDefinitionRenderProps {
    const renderProps: IPropDefinitionRenderProps = {
      datasetVersion,
      enitityType: entityType,
      value: propDefinition.getValue(datasetVersion),
      prop: propDefinition.prop,
      diffInfo: this.props.datasetVersionsDifferentProps[
        propDefinition.prop as keyof IDatasetVersionsDifferentProps
      ],
    };
    return renderProps;
  }
}

export { PropDefinition };
export default CompareDatasetVersionsTable;
