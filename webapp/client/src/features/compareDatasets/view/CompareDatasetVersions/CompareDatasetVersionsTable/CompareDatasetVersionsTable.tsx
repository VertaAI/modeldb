import Paper from '@material-ui/core/Paper';
import * as R from 'ramda';
import React, { useMemo } from 'react';

import { mapObj, groupBy } from 'shared/utils/collection';
import VerticalTable, {
  IPropDefinition as IRowDefinition,
} from 'shared/view/elements/VerticalTable/VerticalTable';
import { IDatasetVersion } from 'shared/models/DatasetVersion';
import { IDatasetVersionsDifferentProps, ComparedDatasetVersions, DatasetVerisonEntityType } from 'features/compareDatasets/store/compareDatasets';

interface ILocalProps {
  datasetVersionsDifferentProps: IDatasetVersionsDifferentProps;
  datasetVersions: Required<ComparedDatasetVersions>;
  columns: {
    [DatasetVerisonEntityType.entity1]: {
      title: string;
    };
    [DatasetVerisonEntityType.entity2]: {
      title: string;
    };
  };
  children: Array<React.ReactElement<IPropDefinition>>;
}

interface IColumnSetting {
  name: DatasetVerisonEntityType.entity1 | DatasetVerisonEntityType.entity2;
  title: string;
  getCellValue?: (row: any, columnName: string) => any;
}

type PropObject<Prop extends IPropDefinition['prop']> = {
  [K in Prop]: IPropDefinition<K>;
};

type PropWithRenderPropsObject<Prop extends IPropDefinition['prop']> = {
  [K in Prop]: IPropDefinitionRenderProps<K>;
};

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
  enitityType: DatasetVerisonEntityType;
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

const getColumnsSettings = (columns: ILocalProps['columns']) => {
  return [
    {
      name: DatasetVerisonEntityType.entity1,
      title: columns[DatasetVerisonEntityType.entity1].title,
      getCellValue: R.identity,
    },
    {
      name: DatasetVerisonEntityType.entity2,
      title: columns[DatasetVerisonEntityType.entity2].title,
      getCellValue: R.identity,
    },
  ];
};

const CompareDatasetVersionsTable: React.FC<ILocalProps> = ({
  columns,
  datasetVersionsDifferentProps,
  datasetVersions,
  children,
}) => {
  const propDefinitions: IPropDefinition[] = useMemo(
    () => getPropDefinitions(children),
    [children]
  );
  const columnsSettings = useMemo(() => getColumnsSettings(columns), [columns]);

  const columnsData = useMemo(
    () =>
      columnsSettings.map((columnSetting) => ({
        columnTitle: columnSetting.title,
        dataName: columnSetting.name,
        data: getColumnData({
          columnSetting,
          propDefinitions,
          datasetVersions,
          datasetVersionsDifferentProps,
        }),
      })),
    [
      columnsSettings,
      propDefinitions,
      datasetVersions,
      datasetVersionsDifferentProps,
    ]
  );

  const rowsDefinitions: Array<IRowDefinition<
    PropWithRenderPropsObject<IPropDefinition['prop']>
  >> = useMemo(
    () =>
      propDefinitions.map((p) => ({
        title: p.title,
        type: p.prop,
        render: (d) => p.render(d[p.prop]),
        titleDataTest: `property-name-${p.prop}`,
        displayOnlyOne: p.prop === ('datasetPathInfos' as any),
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

const getColumnData = ({
  columnSetting,
  propDefinitions,
  datasetVersions,
  datasetVersionsDifferentProps,
}: {
  columnSetting: IColumnSetting;
  propDefinitions: IPropDefinition[];
  datasetVersionsDifferentProps: IDatasetVersionsDifferentProps;
  datasetVersions: Required<ComparedDatasetVersions>;
}) => {
  const propObject = makePropObject({ propDefinitions });
  const propWithRenderPropsObject = makePropWithRenderPropsObject({
    propObject,
    columnSetting,
    datasetVersions,
    datasetVersionsDifferentProps,
  });
  return propWithRenderPropsObject;
};

const makePropObject = ({
  propDefinitions,
}: {
  propDefinitions: IPropDefinition[];
}): PropObject<IPropDefinition['prop']> => {
  const propObject = mapObj(
    (x) => x[0],
    groupBy((p) => p.prop, propDefinitions)
  );

  return propObject as PropObject<IPropDefinition['prop']>;
};

const makePropWithRenderPropsObject = ({
  propObject,
  columnSetting,
  datasetVersionsDifferentProps,
  datasetVersions,
}: {
  propObject: PropObject<IPropDefinition['prop']>;
  columnSetting: IColumnSetting;
  datasetVersionsDifferentProps: IDatasetVersionsDifferentProps;
  datasetVersions: Required<ComparedDatasetVersions>;
}): PropWithRenderPropsObject<IPropDefinition['prop']> => {
  const propWithRenderPropsObject = mapObj(
    (row) =>
      getRenderPropsByRowAndColumn({
        columnSetting,
        row,
        datasetVersions,
        datasetVersionsDifferentProps,
      }),
    propObject
  );
  return propWithRenderPropsObject as PropWithRenderPropsObject<
    IPropDefinition['prop']
  >;
};

const getPropDefinitions = (
  children: Array<React.ReactElement<IPropDefinition>>
): IPropDefinition[] => {
  return R.chain(
    (child: any) =>
      child.type === React.Fragment ? child.props.children : child,
    children
  ).map((child: any) => child.props);
};

const getRenderPropsByRowAndColumn = ({
  columnSetting,
  row: propDefinition,
  datasetVersionsDifferentProps,
  datasetVersions,
}: {
  columnSetting: { name: string };
  row: IPropDefinition;
  datasetVersionsDifferentProps: IDatasetVersionsDifferentProps;
  datasetVersions: Required<ComparedDatasetVersions>;
}) => {
  switch (columnSetting.name) {
    case DatasetVerisonEntityType.entity1: {
      const renderProps = getDatasetVersionPropRenderProps({
        entityType: DatasetVerisonEntityType.entity1,
        datasetVersion: datasetVersions[0],
        propDefinition,
        datasetVersionsDifferentProps,
      });
      return renderProps;
    }
    case DatasetVerisonEntityType.entity2: {
      const renderProps = getDatasetVersionPropRenderProps({
        entityType: DatasetVerisonEntityType.entity2,
        datasetVersion: datasetVersions[1],
        propDefinition,
        datasetVersionsDifferentProps,
      });
      return renderProps;
    }
  }
};

const getDatasetVersionPropRenderProps = ({
  entityType,
  datasetVersion,
  propDefinition,
  datasetVersionsDifferentProps,
}: {
  entityType: DatasetVerisonEntityType;
  datasetVersion: IDatasetVersion;
  propDefinition: IPropDefinition;
  datasetVersionsDifferentProps: IDatasetVersionsDifferentProps;
}): IPropDefinitionRenderProps => {
  const renderProps: IPropDefinitionRenderProps = {
    datasetVersion,
    enitityType: entityType,
    value: propDefinition.getValue(datasetVersion),
    prop: propDefinition.prop,
    diffInfo:
      datasetVersionsDifferentProps[
        propDefinition.prop as keyof IDatasetVersionsDifferentProps
      ],
  };
  return renderProps;
};

export { PropDefinition };
export default CompareDatasetVersionsTable;
