import { ISorting } from 'shared/models/Sorting';

export interface IServerSorting {
  ascending: boolean;
  sort_key: string;
}

export const getServerSorting = (sorting: ISorting): IServerSorting => {
  const sortKey = sorting.columnName
    ? `${sorting.columnName}.${sorting.fieldName}`
    : sorting.fieldName;
  return {
    sort_key: sortKey,
    ascending: sorting.direction === 'asc',
  };
};

export const addSorting = <T>(sorting: ISorting) => (
  request: T
): T & IServerSorting => {
  return { ...request, ...getServerSorting(sorting) };
};
