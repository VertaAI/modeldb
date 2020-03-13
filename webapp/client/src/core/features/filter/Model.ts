import generateId from 'core/shared/utils/generateId';

export enum PropertyType {
  STRING = 'STRING',
  METRIC = 'METRIC',
  ID = 'ID',
  EXPERIMENT_NAME = 'EXPERIMENT_NAME',
  PROJECT_TYPE = 'PROJECT_TYPE',
}

export enum ComparisonType {
  MORE,
  EQUALS,
  LESS,
  GREATER_OR_EQUALS,
  LESS_OR_EQUALS,
}
export interface IStringFilterData {
  id: string;
  type: PropertyType.STRING;
  caption?: string; // custom filter caption. If it is blank will be used `name`
  name: string; // property name
  value: string;
  invert: boolean;
  isFuzzy: boolean;
}

export interface IMetricFilterData {
  id: string;
  type: PropertyType.METRIC;
  caption?: string;
  name: string;
  value: number;
  comparisonType: ComparisonType;
}

export interface IIdFilterData {
  id: string;
  type: PropertyType.ID;
  caption?: string;
  name: 'id';
  value: string[];
  isEdited: boolean;
}

export interface IExperimentNameFilterData {
  id: string;
  type: PropertyType.EXPERIMENT_NAME;
  caption?: string;
  name: string;
  value: string;
  invert: boolean;
}

export type IFilterData =
  | IStringFilterData
  | IMetricFilterData
  | IIdFilterData
  | IExperimentNameFilterData;

export type IInstantFilter = Exclude<IFilterData, IIdFilterData>;

export type IMultipleFilterData = IIdFilterData;

export const makeDefaultIdFilter = (entityId: string): IIdFilterData => {
  return {
    id: generateId(),
    type: PropertyType.ID,
    caption: 'IDs',
    name: 'id',
    value: [entityId],
    isEdited: true,
  };
};

export const makeDefaultStringFilter = (
  name: string,
  value: string,
  isFuzzy: boolean
): IStringFilterData => {
  return {
    isFuzzy,
    id: generateId(),
    type: PropertyType.STRING,
    name,
    value,
    invert: false,
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
    invert: false,
  };
};

// todo refactor
export const makeDefaultMetricFilter = (
  propertyName: string,
  value: number | string
): IMetricFilterData | IStringFilterData => {
  if (typeof value === 'string') {
    return makeDefaultStringFilter(propertyName, value, false);
  }
  return {
    id: generateId(),
    value,
    type: PropertyType.METRIC,
    name: propertyName,
    comparisonType: ComparisonType.GREATER_OR_EQUALS,
  };
};

export const makeDefaultTagFilter = (value: string): IStringFilterData => {
  return { ...makeDefaultStringFilter('tags', value, false), caption: 'tag' };
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
      quickFilter.isFuzzy
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
    propertyName: 'tags',
    isFuzzy: false,
    caption: 'tags',
  },
};
