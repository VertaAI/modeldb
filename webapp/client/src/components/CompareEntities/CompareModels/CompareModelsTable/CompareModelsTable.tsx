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

import ModelRecord from 'models/ModelRecord';
import { IModelsDifferentProps, EntityType } from 'store/compareEntities';

interface ILocalProps {
  modelsDifferentProps: IModelsDifferentProps;
  models: [ModelRecord, ModelRecord];
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
  Prop extends keyof ModelRecord = keyof ModelRecord
> {
  prop: Prop;
  title: string;
  getValue(modelRecord: ModelRecord): ModelRecord[Prop];
  render(settings: IPropDefinitionRenderProps<Prop>): React.ReactNode;
}
export interface IPropDefinitionRenderProps<
  Prop extends keyof ModelRecord | keyof IModelsDifferentProps =
    | keyof ModelRecord
    | keyof IModelsDifferentProps
> {
  currentEntityInfo: IEntityInfo<ModelRecord[Prop]>;
  otherEntityInfo: IEntityInfo<ModelRecord[Prop]>;
  prop: Prop;
  diffInfo: Prop extends keyof IModelsDifferentProps
    ? IModelsDifferentProps[Prop]
    : undefined;
}

interface IEntityInfo<Value> {
  value: Value;
  entityType: EntityType;
  model: ModelRecord;
}

function PropDefinition<Prop extends keyof ModelRecord = keyof ModelRecord>(
  props: IPropDefinition<Prop>
) {
  return null;
}

class CompareModelsTable extends React.Component<ILocalProps, IState> {
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
      case 'propertyName': {
        return (
          <span data-test={`property-name-${propDefinition.prop}`}>
            {propDefinition.title}
          </span>
        );
      }
      case EntityType.entity1: {
        const currentEntityInfo: IEntityInfo<any> = {
          entityType: EntityType.entity1,
          model: this.props.models[0],
          value: propDefinition.getValue(this.props.models[0]),
        };
        const otherEntityInfo: IEntityInfo<any> = {
          entityType: EntityType.entity2,
          model: this.props.models[1],
          value: propDefinition.getValue(this.props.models[1]),
        };
        const renderProps = this.getModelPropRenderProps(
          currentEntityInfo,
          otherEntityInfo,
          propDefinition
        );
        return propDefinition.render(renderProps);
      }
      case EntityType.entity2: {
        const currentEntityInfo: IEntityInfo<any> = {
          entityType: EntityType.entity2,
          model: this.props.models[1],
          value: propDefinition.getValue(this.props.models[1]),
        };
        const otherEntityInfo: IEntityInfo<any> = {
          entityType: EntityType.entity1,
          model: this.props.models[0],
          value: propDefinition.getValue(this.props.models[0]),
        };
        const renderProps = this.getModelPropRenderProps(
          currentEntityInfo,
          otherEntityInfo,
          propDefinition
        );
        return propDefinition.render(renderProps);
      }
    }
  }

  @bind
  private getModelPropRenderProps(
    currentEntityInfo: IEntityInfo<any>,
    otherEntityInfo: IEntityInfo<any>,
    propDefinition: IPropDefinition
  ): IPropDefinitionRenderProps {
    const renderProps: IPropDefinitionRenderProps = {
      currentEntityInfo,
      otherEntityInfo,
      prop: propDefinition.prop,
      diffInfo: this.props.modelsDifferentProps[
        propDefinition.prop as keyof IModelsDifferentProps
      ],
    };
    return renderProps;
  }
}

export { PropDefinition };
export default CompareModelsTable;
