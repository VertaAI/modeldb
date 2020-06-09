import {
  Entities,
  ISearchSettings,
  IResultsSorting,
  defaultResultsSorting,
} from 'core/shared/models/HighLevelSearch';
import { ISorting } from 'core/shared/models/Sorting';
import inferType from 'core/shared/utils/inferType';
import routes, { GetRouteQueryParams } from 'core/shared/routes';
import { updateQueryParamsForLocation } from 'core/shared/utils/updateQueryParams';
import exhaustiveStringTuple from 'core/shared/utils/exhaustiveStringTuple';

import { defaultFilter } from './constants';

export const updateSearchSettingsQueryParams = (
  location: { search: string },
  settings: ISearchSettings
) => {
  return updateQueryParamsForLocation<
    GetRouteQueryParams<typeof routes.highLevelSearch>
  >(location, {
    type: settings.type,
    page: settings.currentPage === 0 ? null : String(settings.currentPage + 1),
    q: settings.nameOrTag ? settings.nameOrTag : '',

    'sorting.direction': settings.sorting
      ? settings.sorting.direction
      : undefined,
    'sorting.field': settings.sorting ? settings.sorting.field : undefined,
  });
};

export const parseSearchSettingsFromPathname = ({
  search,
}: {
  search: string;
}): ISearchSettings => {
  const queryParams = routes.highLevelSearch.parseQueryParams(search);
  return {
    currentPage:
      queryParams && typeof queryParams.page !== 'undefined'
        ? Number(queryParams.page) - 1
        : 0,
    nameOrTag: queryParams && queryParams.q ? queryParams.q : '',
    type:
      queryParams && queryParams.type
        ? stringToEntities(queryParams.type)
        : defaultFilter,
    sorting: queryParams
      ? parseSortingFromURL(queryParams) || defaultResultsSorting
      : defaultResultsSorting,
  };
};

const parseSortingFromURL = (
  sortingFromURL: IURLWithResultsSorting | undefined
): IResultsSorting | undefined => {
  if (!sortingFromURL) {
    return undefined;
  }

  const checkDirection = (
    direction: string
  ): direction is IResultsSorting['direction'] => {
    return (exhaustiveStringTuple<IResultsSorting['direction']>()(
      'asc',
      'desc'
    ) as Array<string>).includes(direction);
  };
  const checkField = (field: string): field is IResultsSorting['field'] => {
    return (exhaustiveStringTuple<IResultsSorting['field']>()(
      'dateCreated',
      'dateUpdated'
    ) as Array<string>).includes(field);
  };

  const direction = sortingFromURL['sorting.direction'];
  const field = sortingFromURL['sorting.field'];
  if (direction && field && checkDirection(direction) && checkField(field)) {
    return { direction, field };
  }
};

const stringToEntities = (str: string): Entities => {
  return inferType<Entities>()(
    {
      projects: () => str === Entities.projects,
      experiments: () => str === Entities.experiments,
      experimentRuns: () => str === Entities.experimentRuns,
      datasets: () => str === Entities.datasets,
      repositories: () => str === Entities.repositories,
    },
    defaultFilter,
    str
  );
};

export interface IURLWithResultsSorting {
  'sorting.direction'?: ISorting['direction'];
  'sorting.field'?: string;
}
