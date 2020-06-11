import { DataWithPagination } from 'core/shared/models/Pagination';

export interface IServerPaginationInRequest {
  page_number: number;
  page_limit: number;
}

export interface IAdditionalServerPaginationInRequest {
  'pagination.page_number': number;
  'pagination.page_limit': number;
}

export type IServerPaginatedResponse<
  DataKey extends string,
  Data = any
> = Partial<Record<DataKey, Data[]>> & {
  total_records?: number;
};

export const convertServerPaginationResponse = <
  Result,
  ServerData = any,
  Response extends IServerPaginatedResponse<
    string,
    ServerData
  > = IServerPaginatedResponse<string, ServerData>
>(
  f: (data: ServerData) => Result,
  getServerData: (server: Response) => ServerData[] | undefined,
  server: Response
): DataWithPagination<Result> => {
  return {
    data: ((getServerData(server) || []) as ServerData[]).map(f),
    totalCount:
      typeof server.total_records === 'undefined'
        ? 0
        : Number(server.total_records),
  };
};
