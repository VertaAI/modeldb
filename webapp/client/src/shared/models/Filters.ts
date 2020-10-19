import generateId from 'shared/utils/generateId';
import { Brand } from 'shared/utils/Brand';
import { Milliseconds } from 'shared/utils/types';

export enum PropertyType {
  STRING = 'STRING',
  METRIC = 'METRIC',
  EXPERIMENT_NAME = 'EXPERIMENT_NAME',
  DATE = 'DATE',
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

export type NumberFilterOperator = Exclude<OperatorType, 'LIKE' | 'NOT_LIKE'>;

export interface IMetricFilterData {
  id: string;
  type: PropertyType.METRIC;
  caption?: string;
  name: string;
  value: number;
  operator: NumberFilterOperator;
  isActive: boolean;
}

export type IExperimentNameFilterData = Omit<IStringFilterData, 'type'> & {
  type: PropertyType.EXPERIMENT_NAME;
};

export type IDateFilterData = Omit<IMetricFilterData, 'type' | 'value'> & {
  value: number | undefined;
  type: PropertyType.DATE;
};

export type IFilterData =
  | IStringFilterData
  | IMetricFilterData
  | IExperimentNameFilterData
  | IDateFilterData;

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

export const makeDefaultDateFilter = (value: Milliseconds): IDateFilterData => {
  return {
    id: generateId(),
    value,
    name: 'dateUpdated',
    type: PropertyType.DATE,
    operator: OperatorType.EQUALS,
    isActive: false,
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

export const makeDefaultDateCreatedFilter = (
  value: Milliseconds
): IDateFilterData => {
  return {
    id: generateId(),
    name: 'date_created',
    type: PropertyType.DATE,
    caption: 'timestamp',
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
  type: PropertyType.STRING | PropertyType.DATE;
  isFuzzy: boolean;
  caption?: string;
}

export const defaultQuickFilters: Record<
  'name' | 'description' | 'tag' | 'owner' | 'timestamp',
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
  owner: {
    type: PropertyType.STRING,
    propertyName: 'owner',
    isFuzzy: false,
    caption: 'owner',
  },
  timestamp: {
    type: PropertyType.DATE,
    propertyName: 'date_created',
    isFuzzy: false,
    caption: 'timestamp',
  },
};

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

export const makeDefaultOwnerFilter = (username: string): IStringFilterData => {
  return makeDefaultStringFilter('owner', username, 'EQUALS');
};
