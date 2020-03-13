import axios from 'axios';

import {
  makeFilterConverter,
  IServerFiltersInRequest,
} from 'core/features/filter/service/serverModel/Filters/converters';
import {
  IFilterData,
  PropertyType,
  IIdFilterData,
  IExperimentNameFilterData,
} from 'core/features/filter/Model';
import { IPagination } from 'core/shared/models/Pagination';
import { ISorting } from 'core/shared/models/Sorting';
import { makeAddFiltersToRequestWithDefaultFilters } from 'features/filter/service/serverModel/Filter/converters';
import { addPaginationToRequest } from 'services/serverModel/Pagination/converters';
import { IServerPaginationInRequest } from 'services/serverModel/Pagination/Pagination';
import { getServerSorting } from 'services/serverModel/Sorting/Sorting';

import {
  ServerFilterValueType,
  getServerFilterOperator,
} from 'core/features/filter/service/serverModel/Filters/Filters';

export type IGetExperimentRunsRequest = {
  project_id: string;
  experiment_id: string;
  experiment_run_ids: string[];

  ascending: boolean;
  sort_key: string;
} & IServerFiltersInRequest &
  IServerPaginationInRequest;

type ITransformedGetExperimentRunsRequest = Partial<IGetExperimentRunsRequest>;
type TransformGetExperimentRunsRequest = (
  request: ITransformedGetExperimentRunsRequest
) => Promise<ITransformedGetExperimentRunsRequest>;

const loadExperimentByName = (projectId: string, experimentName: string) => {
  return axios
    .get('/v1/modeldb/experiment/getExperimentByName', {
      params: {
        name: experimentName,
        project_id: projectId,
      },
    })
    .then(res => res.data.experiment.id)
    .catch(err => 'NULL');
};

const addServerFilters = (
  projectId: string,
  filters: IFilterData[]
): TransformGetExperimentRunsRequest => request => {
  const idFilterConverter = makeFilterConverter<
    ITransformedGetExperimentRunsRequest,
    IIdFilterData
  >({
    predicate: (filter): filter is IIdFilterData =>
      filter.type === PropertyType.ID,
    convert: (filter, resRequest) => {
      return Promise.resolve({
        ...resRequest,
        experiment_run_ids: filter.value,
      });
    },
  });

  const experimentNameFilterConverter = makeFilterConverter<
    ITransformedGetExperimentRunsRequest,
    IExperimentNameFilterData
  >({
    predicate: (filter): filter is IExperimentNameFilterData =>
      filter.type === PropertyType.EXPERIMENT_NAME,
    convert: (filter, resRequest) => {
      return loadExperimentByName(projectId, filter.value).then(
        experimentId => {
          return {
            ...resRequest,
            predicates: (resRequest.predicates || []).concat({
              key: 'experiment_id',
              value: experimentId,
              value_type: ServerFilterValueType.STRING,
              operator: getServerFilterOperator(filter),
            }),
          };
        }
      );
    },
  });

  return makeAddFiltersToRequestWithDefaultFilters([
    idFilterConverter,
    experimentNameFilterConverter,
  ])(filters)(request);
};

const addPagination = (
  pagination: IPagination | null
): TransformGetExperimentRunsRequest => request => {
  return Promise.resolve(
    pagination ? addPaginationToRequest(pagination)(request) : request
  );
};

const addSorting = (
  sorting: ISorting | null
): TransformGetExperimentRunsRequest => request => {
  return Promise.resolve(
    sorting ? { ...request, ...getServerSorting(sorting) } : request
  );
};

const addProjectId = (
  projectId: string
): TransformGetExperimentRunsRequest => request =>
  Promise.resolve({ ...request, project_id: projectId });

const makeLoadExperimentRunsRequest = (
  projectId: string,
  filters: IFilterData[],
  pagination: IPagination | null,
  sorting: ISorting | null
): Promise<IGetExperimentRunsRequest> => {
  return Promise.resolve({})
    .then(addProjectId(projectId))
    .then(addSorting(sorting))
    .then(addPagination(pagination))
    .then(addServerFilters(projectId, filters)) as Promise<
    IGetExperimentRunsRequest
  >;
};

export default makeLoadExperimentRunsRequest;
