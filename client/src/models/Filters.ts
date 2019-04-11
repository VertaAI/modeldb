export enum PropertyType {
  NUMBER,
  STRING,
  METRIC,
  BOOLEAN,
}

export enum ComparisonType {
  MORE,
  EQUALS,
  LESS,
}
export interface IStringFilterData {
  type: PropertyType.STRING;
  caption?: string; // custom filter caption. If it is blank will be used `name`
  name: string; // property name
  value: string;
  invert: boolean;
}

export interface INumberFilterData {
  type: PropertyType.NUMBER;
  caption?: string;
  name: string;
  value: number;
  invert: boolean;
}

export interface IBooleanFilterData {
  type: PropertyType.BOOLEAN;
  caption?: string;
  name: string;
  value: boolean;
}

export interface IMetricFilterData {
  type: PropertyType.METRIC;
  caption?: string;
  name: string;
  value: number;
  comparisonType: ComparisonType;
}

export type IFilterData =
  | IStringFilterData
  | INumberFilterData
  | IBooleanFilterData
  | IMetricFilterData;
