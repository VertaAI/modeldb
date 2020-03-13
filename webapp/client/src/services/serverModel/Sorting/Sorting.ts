import { ISorting } from 'core/shared/models/Sorting';

export interface IServerSorting {
  ascending: boolean;
  sort_key: string;
}

export const getServerSorting = (sorting: ISorting): IServerSorting => {
  const sortKey = `${sorting.columnName}.${sorting.fieldName}`;
  return {
    sort_key: sortKey,
    ascending: sorting.direction === 'asc',
  };
};
