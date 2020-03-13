import {
  getServerFilterValueType,
  getServerFilterOperator,
} from 'core/features/filter/service/serverModel/Filters/Filters';
import { IFilterData } from 'core/features/filter/Model';

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

const defaultFilterConverter = makeFilterConverter<IServerFiltersInRequest>({
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
