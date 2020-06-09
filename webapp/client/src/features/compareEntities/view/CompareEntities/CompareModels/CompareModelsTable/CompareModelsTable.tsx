import Paper from '@material-ui/core/Paper';
import * as R from 'ramda';
import React, { useMemo } from 'react';

import { mapObj, groupBy } from 'core/shared/utils/collection';
import VerticalTable, {
  IPropDefinition as IRowDefinition,
} from 'core/shared/view/elements/VerticalTable/VerticalTable';
import {
  IModelsDifferentProps,
  EntityType,
  IModelRecordView,
} from 'features/compareEntities/store';

interface ILocalProps {
  modelsDifferentProps: IModelsDifferentProps;
  models: Models;
  columns: {
    [EntityType.entity1]: {
      title: string;
    };
    [EntityType.entity2]: {
      title: string;
    };
  };
  children: Array<React.ReactElement<IPropDefinition>>;
}

type Models = [IModelRecordView, IModelRecordView];

type PropObject<Prop extends IPropDefinition['prop']> = {
  [K in Prop]: IPropDefinition<K>
};

type PropWithRenderPropsObject<Prop extends IPropDefinition['prop']> = {
  [K in Prop]: IPropDefinitionRenderProps<K>
};

interface IColumnSetting {
  name: EntityType.entity1 | EntityType.entity2;
  title: string;
  getCellValue?: (row: any, columnName: string) => any;
}

export interface IPropDefinition<
  Prop extends keyof IModelRecordView = keyof IModelRecordView
> {
  prop: Prop;
  title: string;
  getValue(modelRecord: IModelRecordView): IModelRecordView[Prop];
  render(settings: IPropDefinitionRenderProps<Prop>): React.ReactNode;
}
export interface IPropDefinitionRenderProps<
  Prop extends keyof IModelRecordView | keyof IModelsDifferentProps =
    | keyof IModelRecordView
    | keyof IModelsDifferentProps
> {
  currentEntityInfo: IEntityInfo<IModelRecordView[Prop]>;
  otherEntityInfo: IEntityInfo<IModelRecordView[Prop]>;
  prop: Prop;
  diffInfo: Prop extends keyof IModelsDifferentProps
    ? IModelsDifferentProps[Prop]
    : undefined;
}

interface IEntityInfo<Value> {
  value: Value;
  entityType: EntityType;
  model: IModelRecordView;
}

function PropDefinition<
  Prop extends keyof IModelRecordView = keyof IModelRecordView
>(props: IPropDefinition<Prop>) {
  return null;
}

const CompareModelsTable: React.FC<ILocalProps> = ({
  columns,
  models,
  modelsDifferentProps,
  children,
}) => {
  const columnSettings = useMemo(() => getColumnSettings(columns), [columns]);
  const propDefinitions = useMemo(() => getPropDefinitions(children), [
    children,
  ]);

  const columnsData = useMemo(
    () =>
      columnSettings.map(columnSetting => ({
        columnTitle: columnSetting.title,
        dataName: columnSetting.name,
        data: getColumnData({
          columnSetting,
          propDefinitions,
          models,
          modelsDifferentProps,
        }),
      })),
    [columnSettings, propDefinitions, models, modelsDifferentProps]
  );

  const rowsDefinitions: Array<
    IRowDefinition<PropWithRenderPropsObject<IPropDefinition['prop']>>
  > = useMemo(
    () =>
      propDefinitions.map(p => ({
        title: p.title,
        type: p.prop,
        render: d => p.render(d[p.prop]),
        titleDataTest: `property-name-${p.prop}`,
      })),
    [propDefinitions]
  );

  return (
    <Paper>
      <VerticalTable
        columnsData={columnsData}
        propDefinitions={rowsDefinitions}
      />
    </Paper>
  );
};

const getPropDefinitions = (
  children: Array<React.ReactElement<IPropDefinition>>
): IPropDefinition[] => {
  return React.Children.map(
    children,
    (child: React.ReactElement) => child.props
  );
};

const getColumnSettings = (columns: ILocalProps['columns']) => {
  return [
    {
      name: EntityType.entity1,
      title: columns[EntityType.entity1].title,
      getCellValue: R.identity,
    },
    {
      name: EntityType.entity2,
      title: columns[EntityType.entity2].title,
      getCellValue: R.identity,
    },
  ];
};

const getColumnData = ({
  columnSetting,
  propDefinitions,
  modelsDifferentProps,
  models,
}: {
  columnSetting: IColumnSetting;
  propDefinitions: IPropDefinition[];
  modelsDifferentProps: IModelsDifferentProps;
  models: Models;
}) => {
  const propObject = makePropObject({ propDefinitions });
  const propWithRenderPropsObject = makePropWithRenderPropsObject({
    propObject,
    columnSetting,
    models,
    modelsDifferentProps,
  });
  return propWithRenderPropsObject;
};

const makePropObject = ({
  propDefinitions,
}: {
  propDefinitions: IPropDefinition[];
}): PropObject<IPropDefinition['prop']> => {
  const propObject = mapObj(x => x[0], groupBy(p => p.prop, propDefinitions));

  return propObject as PropObject<IPropDefinition['prop']>;
};

const makePropWithRenderPropsObject = ({
  propObject,
  columnSetting,
  models,
  modelsDifferentProps,
}: {
  propObject: PropObject<IPropDefinition['prop']>;
  columnSetting: IColumnSetting;
  models: Models;
  modelsDifferentProps: IModelsDifferentProps;
}): PropWithRenderPropsObject<IPropDefinition['prop']> => {
  const propWithRenderPropsObject = mapObj(
    row =>
      getRenderPropsByRowAndColumn({
        columnSetting,
        row,
        models,
        modelsDifferentProps,
      }),
    propObject
  );
  return propWithRenderPropsObject as PropWithRenderPropsObject<
    IPropDefinition['prop']
  >;
};

const getRenderPropsByRowAndColumn = ({
  columnSetting,
  row: propDefinition,
  models,
  modelsDifferentProps,
}: {
  columnSetting: IColumnSetting;
  row: IPropDefinition;
  models: Models;
  modelsDifferentProps: IModelsDifferentProps;
}) => {
  switch (columnSetting.name) {
    case EntityType.entity1: {
      const currentEntityInfo: IEntityInfo<any> = {
        entityType: EntityType.entity1,
        model: models[0],
        value: propDefinition.getValue(models[0]),
      };
      const otherEntityInfo: IEntityInfo<any> = {
        entityType: EntityType.entity2,
        model: models[1],
        value: propDefinition.getValue(models[1]),
      };
      const renderProps = getModelPropRenderProps({
        currentEntityInfo,
        otherEntityInfo,
        propDefinition,
        modelsDifferentProps,
      });
      return renderProps;
    }
    case EntityType.entity2: {
      const currentEntityInfo: IEntityInfo<any> = {
        entityType: EntityType.entity2,
        model: models[1],
        value: propDefinition.getValue(models[1]),
      };
      const otherEntityInfo: IEntityInfo<any> = {
        entityType: EntityType.entity1,
        model: models[0],
        value: propDefinition.getValue(models[0]),
      };
      const renderProps = getModelPropRenderProps({
        currentEntityInfo,
        otherEntityInfo,
        propDefinition,
        modelsDifferentProps,
      });
      return renderProps;
    }
  }
};

const getModelPropRenderProps = ({
  currentEntityInfo,
  otherEntityInfo,
  propDefinition,
  modelsDifferentProps,
}: {
  currentEntityInfo: IEntityInfo<any>;
  otherEntityInfo: IEntityInfo<any>;
  propDefinition: IPropDefinition;
  modelsDifferentProps: IModelsDifferentProps;
}): IPropDefinitionRenderProps => {
  const renderProps: IPropDefinitionRenderProps = {
    currentEntityInfo,
    otherEntityInfo,
    prop: propDefinition.prop,
    diffInfo:
      modelsDifferentProps[propDefinition.prop as keyof IModelsDifferentProps],
  };
  return renderProps;
};

export { PropDefinition };
export default CompareModelsTable;
