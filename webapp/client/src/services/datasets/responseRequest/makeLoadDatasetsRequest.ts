import { IServerFiltersInRequest } from 'core/features/filter/service/serverModel/Filters/converters';
import { IFilterData } from 'core/features/filter/Model';
import { IPagination } from 'core/shared/models/Pagination';
import { IWorkspace } from 'core/shared/models/Workspace';
import { makeAddFiltersToRequestWithDefaultFilters } from 'features/filter/service/serverModel/Filter/converters';
import { addPaginationToRequest } from 'core/services/serverModel/Pagination/converters';
import { IServerPaginationInRequest } from 'core/services/serverModel/Pagination/Pagination';
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

const addFilters = makeAddFiltersToRequestWithDefaultFilters();

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
