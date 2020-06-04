import * as R from 'ramda';

import generateId from 'core/shared/utils/generateId';
import { Brand } from 'core/shared/utils/Brand';

export enum PropertyType {
  STRING = 'STRING',
  METRIC = 'METRIC',
  EXPERIMENT_NAME = 'EXPERIMENT_NAME',
}

export const OperatorType: { [T in OperatorType]: T } = {
  MORE: 'MORE',
  EQUALS: 'EQUALS',
  NOT_EQUALS: 'NOT_EQUALS',
  LESS: 'LESS',
  GREATER_OR_EQUALS: 'GREATER_OR_EQUALS',
  LESS_OR_EQUALS: 'LESS_OR_EQUALS',
  LIKE: 'LIKE',
  NOT_LIKE: 'NOT_LIKE',
} as const;
export type OperatorType =
  | 'MORE'
  | 'EQUALS'
  | 'NOT_EQUALS'
  | 'LESS'
  | 'GREATER_OR_EQUALS'
  | 'LESS_OR_EQUALS'
  | 'LIKE'
  | 'NOT_LIKE';

export type StringFilterOperator = Extract<
  OperatorType,
  'EQUALS' | 'LIKE' | 'NOT_LIKE' | 'NOT_EQUALS'
>;
export interface IStringFilterData {
  id: string;
  type: PropertyType.STRING;
  caption?: string; // custom filter caption. If it is blank will be used `name`
  name: string; // property name
  value: string;
  operator: StringFilterOperator;
  isActive: boolean;
}

export type MetricFilterOperator = Exclude<OperatorType, 'LIKE' | 'NOT_LIKE'>;
export interface IMetricFilterData {
  id: string;
  type: PropertyType.METRIC;
  caption?: string;
  name: string;
  value: number;
  operator: MetricFilterOperator;
  isActive: boolean;
}

export type ExperimentFilterOperator = Extract<
  OperatorType,
  'EQUALS' | 'NOT_EQUALS'
>;
export interface IExperimentNameFilterData {
  id: string;
  type: PropertyType.EXPERIMENT_NAME;
  caption?: string;
  name: string;
  value: string;
  operator: ExperimentFilterOperator;
  isActive: boolean;
}

export type IFilterData =
  | IStringFilterData
  | IMetricFilterData
  | IExperimentNameFilterData;

export const makeDefaultStringFilter = (
  name: string,
  value: string,
  operator: StringFilterOperator
): IStringFilterData => {
  return {
    operator,
    id: generateId(),
    type: PropertyType.STRING,
    name,
    value,
    isActive: true,
  };
};

export const makeDefaultExprNameFilter = (
  value: string
): IExperimentNameFilterData => {
  return {
    id: generateId(),
    name: 'experiment.name',
    type: PropertyType.EXPERIMENT_NAME,
    caption: 'experiment',
    value,
    operator: 'EQUALS',
    isActive: true,
  };
};

// todo refactor
export const makeDefaultMetricFilter = (
  propertyName: string,
  value: number | string
): IMetricFilterData | IStringFilterData => {
  if (typeof value === 'string') {
    return makeDefaultStringFilter(propertyName, value, 'EQUALS');
  }
  return {
    id: generateId(),
    value,
    type: PropertyType.METRIC,
    name: propertyName,
    operator: OperatorType.GREATER_OR_EQUALS,
    isActive: true,
  };
};

export const makeDefaultNameFilter = (
  value: string,
  operator: StringFilterOperator = 'LIKE'
) => {
  return makeDefaultStringFilter('name', value, operator);
};

export const makeDefaultTagFilter = (
  value: string,
  operator: StringFilterOperator = 'EQUALS'
): IStringFilterData => {
  return {
    ...makeDefaultStringFilter('tags', value, operator),
    caption: 'tag',
  };
};

export interface IQuickFilter {
  propertyName: string;
  type: PropertyType.STRING;
  isFuzzy: boolean;
  caption?: string;
}

export const makeDefaultFilterDataFromQuickFilter = (
  quickFilter: IQuickFilter,
  value: string
): IStringFilterData => {
  return {
    ...makeDefaultStringFilter(
      quickFilter.propertyName,
      value,
      quickFilter.isFuzzy ? 'LIKE' : 'EQUALS'
    ),
    caption: quickFilter.caption,
  };
};

export const defaultQuickFilters: Record<
  'name' | 'description' | 'tag',
  IQuickFilter
> = {
  name: {
    type: PropertyType.STRING,
    propertyName: 'name',
    caption: 'name',
    isFuzzy: true,
  },
  description: {
    type: PropertyType.STRING,
    propertyName: 'description',
    caption: 'description',
    isFuzzy: true,
  },
  tag: {
    type: PropertyType.STRING,
    propertyName: 'tag',
    isFuzzy: false,
    caption: 'tag',
  },
};

//

declare const URLFiltersSymbol: unique symbol;
export type URLFilters = Brand<string, 'URLFilters', typeof URLFiltersSymbol>;
export const makeURLFilters = (filters: IFilterData[]): URLFilters => {
  return encodeURIComponent(JSON.stringify(filters)) as URLFilters;
};
export const convertURLFilters = (urlFilters: URLFilters): IFilterData =>
  JSON.parse(decodeURIComponent(urlFilters));

export const URLFiltersParam: keyof IURLWithFilters = 'filters';
export interface IURLWithFilters {
  filters: URLFilters;
}
