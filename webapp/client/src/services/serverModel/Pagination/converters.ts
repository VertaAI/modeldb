import { IPagination } from 'core/shared/models/Pagination';
import { IServerPaginationInRequest } from 'services/serverModel/Pagination/Pagination';

export const addPaginationToRequest = <
  T extends Partial<IServerPaginationInRequest>
>(
  pagination: IPagination
) => (request: T): IServerPaginationInRequest => {
  return { ...request, ...convertClientPagination(pagination) };
};

export const convertClientPagination = (pagination: IPagination) => {
  return {
    page_number: pagination.currentPage + 1,
    page_limit: pagination.pageSize,
  };
};
