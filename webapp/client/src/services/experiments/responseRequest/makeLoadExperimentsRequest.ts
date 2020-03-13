import { IServerFiltersInRequest } from 'core/features/filter/service/serverModel/Filters/converters';
import { IFilterData } from 'core/features/filter/Model';
import { IPagination } from 'core/shared/models/Pagination';
import { makeAddFiltersToRequestWithDefaultFilters } from 'features/filter/service/serverModel/Filter/converters';
import { addPaginationToRequest } from 'services/serverModel/Pagination/converters';
import { IServerPaginationInRequest } from 'services/serverModel/Pagination/Pagination';

export type IGetExperimentsRequest = {
  project_id: string;
  experiment_ids: string[];
} & IServerFiltersInRequest &
  IServerPaginationInRequest;

type ITransformedGetExperimentRunsRequest = Partial<IGetExperimentsRequest>;
type TransformGetExperimentRunsRequest = (
  request: ITransformedGetExperimentRunsRequest
) => Promise<ITransformedGetExperimentRunsRequest>;

const addProjectId = (
  projectId: string
): TransformGetExperimentRunsRequest => request =>
  Promise.resolve({
    ...request,
    project_id: projectId,
  });

const addPagination = (
  pagination: IPagination
): TransformGetExperimentRunsRequest => request =>
  Promise.resolve(addPaginationToRequest(pagination)(request));

const addFilters = makeAddFiltersToRequestWithDefaultFilters();

const makeGetExperimentsRequest = (
  projectId: string,
  filters: IFilterData[],
  pagination: IPagination
): Promise<IGetExperimentsRequest> => {
  return Promise.resolve({})
    .then(addProjectId(projectId))
    .then(addPagination(pagination))
    .then(addFilters(filters)) as Promise<IGetExperimentsRequest>;
};

export default makeGetExperimentsRequest;
