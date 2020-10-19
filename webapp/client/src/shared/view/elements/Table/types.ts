import React from 'react';

export type DataGroup<T> = T[];

export interface IAdditionalClassNames {
  cell?: string;
  headerCell?: string;
  headerRow?: string;
  row?: string;
  group?: string;
  root?: string;
}

export type CustomSortLabel = React.ComponentType<{
  children: React.ReactNode;
}>;

export interface ICommonColumnDefinition<T> {
  title: string;
  type: string;
  getCellStyle?: (row: T) => object;
  render: (row: T) => Exclude<React.ReactNode, undefined>;
  width: string;
  align?: ColumnAlign;
}

export type ColumnAlign = 'left' | 'right' | 'center' | undefined;

export interface IColumnDefinitionWithoutSort<T>
  extends ICommonColumnDefinition<T> {
  withSort?: false;
}

export interface IColumnDefinitionWithSort<T>
  extends ICommonColumnDefinition<T> {
  withSort: true;
  getValue?: (row: T) => string | number;
  customSortLabel?: CustomSortLabel;
}

export type ColumnDefinition<T> =
  | IColumnDefinitionWithoutSort<T>
  | IColumnDefinitionWithSort<T>;

export interface ISelection<T> {
  headerCellComponent: () => React.ReactNode;
  cellComponent: (row: T) => React.ReactNode;
  showSelectAll: boolean;
  showSelectionColumn: boolean;
}

export interface IRowStyle {
  display: string;
  gridTemplateColumns: string;
}

export interface IGroup<T> {
  rows: Array<IRow<T>>;
  style: object;
  key: string | number;
}

export interface IRow<T> {
  data: T;
  cells: Array<ICell<T>>;
  style: object;
  key: string | number;
}

export interface ICell<T> {
  render: (data: T) => React.ReactNode;
  style?: object;
  dataType?: string;
  key: string | number;
  align: ColumnAlign;
}
