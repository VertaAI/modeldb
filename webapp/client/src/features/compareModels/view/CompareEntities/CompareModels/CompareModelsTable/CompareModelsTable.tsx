import Paper from '@material-ui/core/Paper';
import * as R from 'ramda';
import React, { useMemo } from 'react';

import { mapObj, groupBy } from 'shared/utils/collection';
import VerticalTable, {
  IPropDefinition as IRowDefinition,
} from 'shared/view/elements/VerticalTable/VerticalTable';
import { IModelsDifferentProps, IModelDifferentProps, ComparedMultipleModels, ComparedModelRecord } from 'features/compareModels/store/compareModels/compareModels';

interface ILocalProps {
  modelsDifferentProps: IModelsDifferentProps;
  models: ComparedMultipleModels;
  columns: Record<number, { title: string }>;
  children: Array<React.ReactElement<IPropDefinition>>;
}

type PropObject<Prop extends IPropDefinition['prop']> = {
  [K in Prop]: IPropDefinition<K>
};

type PropWithRenderPropsObject<Prop extends IPropDefinition['prop']> = {
  [K in Prop]: IPropDefinitionRenderProps<K>
};

interface IColumnSetting {
  name: number;
  title: string;
  getCellValue?: (row: any, columnName: string) => any;
}

export interface IPropDefinition<
  Prop extends keyof IModelDifferentProps = keyof IModelDifferentProps
> {
  prop: Prop;
  title: string;
  getValue(modelRecord: ComparedModelRecord): ComparedModelRecord[Prop];
  render(settings: IPropDefinitionRenderProps<Prop, any>): React.ReactNode;
}
export interface IPropDefinitionRenderProps<
  Prop extends keyof IModelDifferentProps = keyof IModelDifferentProps,
  DiffInfo extends IModelDifferentProps[Prop] = IModelDifferentProps[Prop]
> {
  currentEntityInfo: IEntityInfo<ComparedModelRecord[Prop], DiffInfo>;
  otherEntitiesInfo: Array<IEntityInfo<ComparedModelRecord[Prop], DiffInfo>>;
  allEntitiesInfo: Array<IEntityInfo<ComparedModelRecord[Prop], DiffInfo>>;
  prop: Prop;
}

export interface IEntityInfo<Value, Diff extends IModelDifferentProps[keyof IModelDifferentProps]> {
  value: Value;
  entityType: number;
  model: ComparedModelRecord;
  diffInfo: Diff;
}

function PropDefinition<
  Prop extends keyof IModelDifferentProps = keyof IModelDifferentProps,
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

const getColumnSettings = (columns: ILocalProps['columns']): IColumnSetting[] => {
  return Object.entries(columns)
    .map(([modelNumber, column]) => ({
      name: Number(modelNumber),
      title: column.title,
      getCellValue: R.identity,
    }));
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
  models: ComparedMultipleModels;
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
  models: ComparedMultipleModels;
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
  models: ComparedMultipleModels;
  modelsDifferentProps: IModelsDifferentProps;
}) => {
  const currentModel = models.find(({ modelNumber }) => modelNumber === columnSetting.name)!;
  const currentEntityInfo: IEntityInfo<any, any> = {
    entityType: columnSetting.name,
    model: currentModel,
    value: propDefinition.getValue(currentModel),
    diffInfo: modelsDifferentProps[currentModel.id][propDefinition.prop],
  };

  const otherEntitiesInfo = models
    .filter(({ modelNumber }) => modelNumber !== columnSetting.name)
    .map((otherModel) => {
      const otherEntityInfo: IEntityInfo<any, any> = ({
        entityType: otherModel.modelNumber!,
        model: otherModel,
        value: propDefinition.getValue(otherModel),
        diffInfo: modelsDifferentProps[otherModel.id][propDefinition.prop],
      });
      return otherEntityInfo;
    });

  const renderProps: IPropDefinitionRenderProps = {
    currentEntityInfo,
    otherEntitiesInfo,
    allEntitiesInfo: models.map(model => ({
      entityType: model.modelNumber!,
      model: model,
      value: propDefinition.getValue(model),
      diffInfo: modelsDifferentProps[model.id][propDefinition.prop],
    })),
    prop: propDefinition.prop,
  };
  return renderProps;
};

export { PropDefinition };
export default CompareModelsTable;
