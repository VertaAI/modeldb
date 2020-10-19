import moment from 'moment';

import {
  IFilterData,
  IDateFilterData,
  PropertyType,
} from 'shared/models/Filters';
import {
  getServerFilterValueType,
  getServerFilterOperator,
  ServerFilterOperator,
} from 'services/serverModel/Filters/Filters';
import { exhaustiveCheck } from 'shared/utils/exhaustiveCheck';
import { Milliseconds } from 'shared/utils/types';

export interface IFilterConverter<
  R extends IServerFiltersInRequest,
  T extends IFilterData = IFilterData
  > {
  predicate: (filter: IFilterData) => filter is T;
  convert: (filter: T, request: R) => Promise<R>;
}
export interface IServerFiltersInRequest {
  predicates?: any[];
}
export const makeFilterConverter = <
  R extends IServerFiltersInRequest,
  T extends IFilterData = IFilterData
>(
  filterConverter: IFilterConverter<R, T>
) => filterConverter;

export const defaultFilterConverter = makeFilterConverter<
  IServerFiltersInRequest
>({
  predicate: (_): _ is IFilterData => true,
  convert: (filter, request) => {
    return Promise.resolve({
      ...request,
      predicates: (request.predicates || []).concat({
        key: filter.name.toLocaleLowerCase() === 'tag' ? 'tags' : filter.name,
        value: Array.isArray(filter.value) ? filter.value[0] : filter.value,
        value_type: getServerFilterValueType(filter.type),
        operator: getServerFilterOperator(filter),
      }),
    });
  },
});

export const dateFilterConverter = makeFilterConverter<
  IServerFiltersInRequest,
  IDateFilterData
>({
  predicate: (filter): filter is IDateFilterData =>
    filter.type === PropertyType.DATE,
  convert: (filter, request) => {
    return Promise.resolve({
      ...request,
      predicates: (() => {
        return typeof filter.value !== 'undefined'
          ? getDatePredicatesWithValueAndOperator(
            filter as Omit<IDateFilterData, 'value'> & { value: number }
          ).map(valueAndOperator => ({
            ...valueAndOperator,
            key: filter.name,
            value_type: getServerFilterValueType(filter.type),
          }))
          : [];
      })(),
    });
  },
});

const getDatePredicatesWithValueAndOperator = (
  filter: Omit<IDateFilterData, 'value'> & { value: number }
): Array<{ value: Milliseconds; operator: ServerFilterOperator }> => {
  switch (filter.operator) {
    case 'EQUALS': {
      return [
        {
          value: +moment(filter.value).set('milliseconds', 0),
          operator: ServerFilterOperator.GTE,
        },
        {
          value: +moment(filter.value).set('milliseconds', 999),
          operator: ServerFilterOperator.LTE,
        },
      ];
    }
    case 'GREATER_OR_EQUALS': {
      return [
        {
          value: +moment(filter.value).set('milliseconds', 0),
          operator: ServerFilterOperator.GTE,
        },
      ];
    }
    case 'MORE': {
      return [
        {
          value: +moment(filter.value).set('milliseconds', 999),
          operator: ServerFilterOperator.GT,
        },
      ];
    }
    case 'LESS_OR_EQUALS': {
      return [
        {
          value: +moment(filter.value).set('milliseconds', 999),
          operator: ServerFilterOperator.LTE,
        },
      ];
    }
    case 'LESS': {
      return [
        {
          value: +moment(filter.value).set('milliseconds', 0),
          operator: ServerFilterOperator.LT,
        },
      ];
    }
    case 'NOT_EQUALS': {
      return [
        {
          value: filter.value,
          operator: ServerFilterOperator.NE,
        },
      ];
    }
    default:
      return exhaustiveCheck(filter.operator, '');
  }
};

export const makeAddFiltersToRequest: <R extends IServerFiltersInRequest>(
  additionalFilterConverters?: Array<IFilterConverter<R, any>>
) => (filters: IFilterData[]) => (request: R) => Promise<R> = (
  additionalFilterConverters = []
) => filters => request => {
  const filterConverters = [
    ...additionalFilterConverters,
    defaultFilterConverter,
  ];

  return filters
    .map(filter => {
      const appropriateFilterConverter = filterConverters.find(
        ({ predicate }) => predicate(filter)
      );
      if (appropriateFilterConverter) {
        return [filter, appropriateFilterConverter.convert] as [
          IFilterData,
          IFilterConverter<any>['convert']
        ];
      }
      throw new Error(`not found converter for filter = ${filter}`);
    })
    .reduce((resPromise, [filter, convert]) => {
      return resPromise.then(resRequest => convert(filter, resRequest));
    }, Promise.resolve(request));
};

export const makeAddFiltersToRequestWithDefaultFilters = <
  R extends IServerFiltersInRequest
>(
  additionalFilterConverters: Array<IFilterConverter<R, any>> = []
) => {
  const filtersConverters = [
    dateFilterConverter,
    ...additionalFilterConverters,
  ];

  return makeAddFiltersToRequest<R>(filtersConverters as any);
};
