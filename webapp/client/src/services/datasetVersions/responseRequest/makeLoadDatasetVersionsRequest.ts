import { IServerFiltersInRequest } from 'core/features/filter/service/serverModel/Filters/converters';
import { IFilterData } from 'core/features/filter/Model';
import { IPagination } from 'core/shared/models/Pagination';
import { makeAddFiltersToRequestWithDefaultFilters } from 'features/filter/service/serverModel/Filter/converters';
import { addPaginationToRequest } from 'services/serverModel/Pagination/converters';
import { IServerPaginationInRequest } from 'services/serverModel/Pagination/Pagination';

export type ILoadDatasetsRequest = {
  dataset_id: string;
  dataset_versions_ids: string[];
} & IServerFiltersInRequest &
  IServerPaginationInRequest;

type ITransformedLoadDatasetsRequest = Partial<ILoadDatasetsRequest>;
type TransformLoadDatasetsRequest = (
  request: ITransformedLoadDatasetsRequest
) => Promise<ITransformedLoadDatasetsRequest>;

const addPagination = (
  pagination: IPagination
): TransformLoadDatasetsRequest => request =>
  Promise.resolve(addPaginationToRequest(pagination)(request));

const addFilters = makeAddFiltersToRequestWithDefaultFilters();

const addDatasetId = (
  datasetId: string
): TransformLoadDatasetsRequest => request =>
  Promise.resolve({
    ...request,
    dataset_id: datasetId,
  });

const makeLoadDatasetVersionsRequest = (
  datasetId: string,
  filters: IFilterData[],
  pagination: IPagination
): Promise<ILoadDatasetsRequest> => {
  return Promise.resolve({})
    .then(addDatasetId(datasetId))
    .then(addPagination(pagination))
    .then(addFilters(filters)) as Promise<ILoadDatasetsRequest>;
};

export default makeLoadDatasetVersionsRequest;
