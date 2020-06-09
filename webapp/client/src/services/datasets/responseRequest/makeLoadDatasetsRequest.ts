import { IServerFiltersInRequest } from 'services/serverModel/Filters/converters';
import { IFilterData } from 'core/shared/models/Filters';
import { IPagination } from 'core/shared/models/Pagination';
import { IWorkspace } from 'core/shared/models/Workspace';
import { makeAddFiltersToRequest } from 'services/serverModel/Filters/converters';
import { addPaginationToRequest } from 'services/serverModel/Pagination/converters';
import { IServerPaginationInRequest } from 'services/serverModel/Pagination/Pagination';
import {
  IServerEntityWithWorkspaceName,
  addWorkspaceName,
} from 'services/serverModel/Workspace/converters';
import { ISorting } from 'core/shared/models/Sorting';
import { addSorting } from 'services/serverModel/Sorting/Sorting';

export type ILoadDatasetsRequest = {
  dataset_ids: string[];
} & IServerPaginationInRequest &
  IServerFiltersInRequest &
  IServerEntityWithWorkspaceName;

type ITransformedLoadDatasetsRequest = Partial<ILoadDatasetsRequest>;
type TransformLoadDatasetsRequest = (
  request: ITransformedLoadDatasetsRequest
) => Promise<ITransformedLoadDatasetsRequest>;

const addPagination = (
  pagination: IPagination
): TransformLoadDatasetsRequest => request =>
  Promise.resolve(addPaginationToRequest(pagination)(request));

const addFilters = makeAddFiltersToRequest();

const makeLoadDatasetsRequest = (
  filters: IFilterData[],
  pagination: IPagination,
  workspaceName?: IWorkspace['name'],
  sorting?: ISorting
): Promise<ILoadDatasetsRequest> => {
  return Promise.resolve({})
    .then(addPagination(pagination))
    .then(workspaceName ? addWorkspaceName(workspaceName) : request => request)
    .then(addFilters(filters))
    .then(sorting ? addSorting(sorting) : request => request) as Promise<
    ILoadDatasetsRequest
  >;
};

export default makeLoadDatasetsRequest;
