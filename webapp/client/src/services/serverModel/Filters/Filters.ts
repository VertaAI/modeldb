import { IFilterData, PropertyType, OperatorType } from 'shared/models/Filters';
import { exhaustiveCheck } from 'shared/utils/exhaustiveCheck';
import matchBy from 'shared/utils/matchBy';

export interface IServerFilter {
  key: string;
  value: string | number;
  value_type: ServerFilterValueType;
  operator: ServerFilterOperator;
}

export enum ServerFilterOperator {
  EQ = 'EQ',
  NE = 'NE',
  GT = 'GT',
  GTE = 'GTE',
  LT = 'LT',
  LTE = 'LTE',
  CONTAIN = 'CONTAIN',
  NOT_CONTAIN = 'NOT_CONTAIN',
}

export const getServerFilterOperator = (
  filter: IFilterData
): ServerFilterOperator => {
  return matchBy(filter, 'type')({
    STRING: stringFilter => {
      switch (stringFilter.operator) {
        case OperatorType.EQUALS:
          return ServerFilterOperator.EQ;
        case OperatorType.LIKE:
          return ServerFilterOperator.CONTAIN;
        case OperatorType.NOT_LIKE:
          return ServerFilterOperator.NOT_CONTAIN;
        case OperatorType.NOT_EQUALS:
          return ServerFilterOperator.NE;
        default:
          return exhaustiveCheck(stringFilter.operator, '');
      }
    },
    EXPERIMENT_NAME: experimentFilter => {
      switch (experimentFilter.operator) {
        case OperatorType.NOT_EQUALS:
          return ServerFilterOperator.NE;
        case OperatorType.EQUALS:
          return ServerFilterOperator.EQ;
        case OperatorType.LIKE:
          return ServerFilterOperator.CONTAIN;
        case OperatorType.NOT_LIKE:
          return ServerFilterOperator.NOT_CONTAIN;
        default:
          return exhaustiveCheck(experimentFilter.operator, '');
      }
    },
    METRIC: metricFilter => {
      switch (metricFilter.operator) {
        case OperatorType.EQUALS:
          return ServerFilterOperator.EQ;
        case OperatorType.MORE:
          return ServerFilterOperator.GT;
        case OperatorType.GREATER_OR_EQUALS:
          return ServerFilterOperator.GTE;
        case OperatorType.LESS:
          return ServerFilterOperator.LT;
        case OperatorType.LESS_OR_EQUALS:
          return ServerFilterOperator.LTE;
        case OperatorType.NOT_EQUALS:
          return ServerFilterOperator.NE;
        default:
          return exhaustiveCheck(metricFilter.operator, '');
      }
    },
    DATE: metricFilter => {
      switch (metricFilter.operator) {
        case OperatorType.EQUALS:
          return ServerFilterOperator.EQ;
        case OperatorType.MORE:
          return ServerFilterOperator.GT;
        case OperatorType.GREATER_OR_EQUALS:
          return ServerFilterOperator.GTE;
        case OperatorType.LESS:
          return ServerFilterOperator.LT;
        case OperatorType.LESS_OR_EQUALS:
          return ServerFilterOperator.LTE;
        case OperatorType.NOT_EQUALS:
          return ServerFilterOperator.NE;
        default:
          return exhaustiveCheck(metricFilter.operator, '');
      }
    },
  });
};

export enum ServerFilterValueType {
  STRING = 'STRING',
  NUMBER = 'NUMBER',
  LIST = 'LIST',
  BLOB = 'BLOB',
}

export const getServerFilterValueType = (
  type: PropertyType
): ServerFilterValueType => {
  switch (type) {
    case PropertyType.STRING:
    case PropertyType.EXPERIMENT_NAME:
      return ServerFilterValueType.STRING;
    case PropertyType.DATE:
    case PropertyType.METRIC:
      return ServerFilterValueType.NUMBER;
    default:
      return ServerFilterValueType.STRING;
  }
};