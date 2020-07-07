import { IServerFiltersInRequest } from 'services/serverModel/Filters/converters';
import { IFilterData } from 'shared/models/Filters';
import { IPagination } from 'shared/models/Pagination';
import { IWorkspace } from 'shared/models/Workspace';
import { makeAddFiltersToRequest } from 'services/serverModel/Filters/converters';
import { addPaginationToRequest } from 'services/serverModel/Pagination/converters';
import { IServerPaginationInRequest } from 'services/serverModel/Pagination/Pagination';
import {
  addWorkspaceName,
  IServerEntityWithWorkspaceName,
} from 'services/serverModel/Workspace/converters';
import { ISorting } from 'shared/models/Sorting';
import { addSorting } from 'services/serverModel/Sorting/Sorting';

export type ILoadProjectsRequest = IServerFiltersInRequest &
  IServerPaginationInRequest &
  IServerEntityWithWorkspaceName;
type ITransformedLoadProjectsRequest = Partial<ILoadProjectsRequest>;

const addFilters = makeAddFiltersToRequest<ITransformedLoadProjectsRequest>();

const makeLoadProjectsRequest = (
  filters: IFilterData[],
  pagination?: IPagination,
  workspaceName?: IWorkspace['name'],
  sorting?: ISorting
): Promise<ILoadProjectsRequest> => {
  return Promise.resolve({})
    .then(
      pagination
        ? addPaginationToRequest<ITransformedLoadProjectsRequest>(pagination)
        : pagination
    )
    .then(workspaceName ? addWorkspaceName(workspaceName) : request => request)
    .then(addFilters(filters))
    .then(sorting ? addSorting(sorting) : request => request) as Promise<
    ILoadProjectsRequest
  >;
};

export default makeLoadProjectsRequest;
