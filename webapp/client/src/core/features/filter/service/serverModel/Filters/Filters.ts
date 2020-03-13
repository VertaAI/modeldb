import {
  IFilterData,
  PropertyType,
  ComparisonType,
} from 'core/features/filter/Model';

export interface IServerFilter {
  key: string;
  value: string | number;
  value_type: ServerFilterValueType;
  operator: ServerFilterOperator;
}

export enum ServerFilterOperator {
  EQ = 0,
  NE = 1,
  GT = 2,
  GTE = 3,
  LT = 4,
  LTE = 5,
  CONTAIN = 6,
  NOT_CONTAIN = 7,
}

export const getServerFilterOperator = (
  filter: IFilterData
): ServerFilterOperator => {
  if (filter.type === PropertyType.METRIC) {
    switch (filter.comparisonType) {
      case ComparisonType.EQUALS:
        return ServerFilterOperator.EQ;
      case ComparisonType.MORE:
        return ServerFilterOperator.GT;
      case ComparisonType.GREATER_OR_EQUALS:
        return ServerFilterOperator.GTE;
      case ComparisonType.LESS:
        return ServerFilterOperator.LT;
      case ComparisonType.LESS_OR_EQUALS:
        return ServerFilterOperator.LTE;
    }
  }
  if (filter.type === PropertyType.STRING && filter.isFuzzy) {
    return filter.invert
      ? ServerFilterOperator.NOT_CONTAIN
      : ServerFilterOperator.CONTAIN;
  }
  if ('invert' in filter) {
    return filter.invert ? ServerFilterOperator.NE : ServerFilterOperator.EQ;
  }
  return ServerFilterOperator.EQ;
};

export enum ServerFilterValueType {
  STRING = 0,
  NUMBER = 1,
  LIST = 2,
  BLOB = 3,
}

export const getServerFilterValueType = (
  type: PropertyType
): ServerFilterValueType => {
  switch (type) {
    case PropertyType.ID:
    case PropertyType.STRING:
    case PropertyType.EXPERIMENT_NAME:
      return ServerFilterValueType.STRING;
    case PropertyType.METRIC:
      return ServerFilterValueType.NUMBER;
    default:
      return ServerFilterValueType.STRING;
  }
};
