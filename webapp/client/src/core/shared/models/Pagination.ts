export interface IPagination {
  currentPage: number;
  pageSize: number;
  totalCount: number;
}

export interface DataWithPagination<T> {
  data: T[];
  totalCount: number;
}

export const substractPaginationTotalCount = (
  n: number,
  pagination: IPagination
): IPagination => {
  const paginationWithUpdatedTotalCount: IPagination = {
    ...pagination,
    totalCount: pagination.totalCount - n,
  };
  const newPageCount = getPaginationPageCount(paginationWithUpdatedTotalCount);
  return {
    ...paginationWithUpdatedTotalCount,
    currentPage:
      newPageCount - 1 >= 0 &&
      paginationWithUpdatedTotalCount.currentPage > newPageCount - 1
        ? newPageCount - 1
        : paginationWithUpdatedTotalCount.currentPage,
  };
};

export const getPaginationPageCount = (pagination: IPagination): number =>
  Math.ceil(pagination.totalCount / pagination.pageSize);
