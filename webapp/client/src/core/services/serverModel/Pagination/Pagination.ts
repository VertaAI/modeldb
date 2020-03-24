export interface IServerPaginationInRequest {
  page_number: number;
  page_limit: number;
}

export interface IAdditionalServerPaginationInRequest {
  'pagination.page_number': number;
  'pagination.page_limit': number;
}
