import { IServerFiltersInRequest } from 'core/features/filter/service/serverModel/Filters/converters';
import { IFilterData } from 'core/features/filter/Model';
import { IPagination } from 'core/shared/models/Pagination';
import { makeAddFiltersToRequestWithDefaultFilters } from 'features/filter/service/serverModel/Filter/converters';
import { addPaginationToRequest } from 'core/services/serverModel/Pagination/converters';
import { IServerPaginationInRequest } from 'core/services/serverModel/Pagination/Pagination';
import { IWorkspace } from 'core/shared/models/Workspace';
import { addWorkspaceName } from 'services/serverModel/Workspace/converters';
import { ISorting } from 'core/shared/models/Sorting';
import { addSorting } from 'services/serverModel/Sorting/Sorting';

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

const makeGetExperimentsByWorkspaceRequest = ({
  filters,
  pagination,
  workspaceName,
  sorting,
}: {
  workspaceName: IWorkspace['name'];
  filters: IFilterData[];
  pagination: IPagination;
  sorting?: ISorting;
}) => {
  return Promise.resolve({})
    .then(addWorkspaceName(workspaceName))
    .then(addPagination(pagination) as any)
    .then(addFilters(filters) as any)
    .then(sorting ? addSorting(sorting) : request => request);
};

export { makeGetExperimentsByWorkspaceRequest };

export default makeGetExperimentsRequest;
