import * as R from 'ramda';

import { BaseDataService } from 'services/BaseDataService';
import { EntityErrorType } from 'shared/models/Common';
import { HttpError } from 'shared/models/Error';
import { IFilterData } from 'shared/models/Filters';
import { DataWithPagination, IPagination } from 'shared/models/Pagination';
import * as Dataset from 'shared/models/Dataset';
import { IWorkspace } from 'shared/models/Workspace';
import { convertServerEntityWithLoggedDates } from 'services/serverModel/Common/converters';

import { convertServerShortWorkspaceToClient } from 'services/serverModel/Workspace/converters';
import * as EntityAlreadyExistError from '../shared/EntityAlreadyExistError';
import makeLoadDatasetsRequest from './responseRequest/makeLoadDatasetsRequest';
import { ISorting } from 'shared/models/Sorting';
import {
  convertServerPaginationResponse,
  IServerPaginatedResponse,
} from 'services/serverModel/Pagination/Pagination';

const convertDatasetVisibilityToServer = (
  datasetVisibility: Dataset.DatasetVisibility
): number => {
  const record: Record<Dataset.DatasetVisibility, number> = {
    private: 0,
    // tobe enabled after a backend update
    // public: 1,
  };
  return record[datasetVisibility];
};

export default class DatasetsDataService extends BaseDataService {
  public async createDataset(
    settings: Dataset.IDatasetCreationSettings
  ): Promise<Dataset.Dataset> {
    const request = (() => {
      const requestFields: {
        [K in keyof Omit<
          Required<Dataset.IDatasetCreationSettings>,
          'workspaceName'
        >]: [string, any];
      } = {
        name: ['name', settings.name],
        visibility: [
          'dataset_visibility',
          convertDatasetVisibilityToServer(settings.visibility),
        ],
        description: ['description', settings.description],
        tags: ['tags', settings.tags],
        type: ['dataset_type', settings.type.toUpperCase()],
      };
      return R.fromPairs(R.values(requestFields));
    })();

    const response = await this.post<
      any,
      EntityAlreadyExistError.EntityAlreadyExistsErrorType
    >({
      url: `/v1/modeldb/dataset/createDataset`,
      data: request,
      errorConverters: {
        entityAlreadyExists: (errorResponse) => errorResponse.status === 409,
      },
    });
    return this.loadDataset(response.data.dataset.id, settings.workspaceName);
  }

  public async loadDatasets(
    filters: IFilterData[],
    pagination: IPagination,
    workspaceName: IWorkspace['name'],
    sorting?: ISorting
  ): Promise<DataWithPagination<Dataset.Dataset>> {
    const request = await makeLoadDatasetsRequest(
      filters,
      pagination,
      workspaceName,
      sorting
    );
    const response = await this.post<IServerDatasetsResponse>({
      url: '/v1/modeldb/hydratedData/findHydratedDatasets',
      data: request,
    });
    return convertServerPaginationResponse(
      convertServerHydratedDataset,
      (d) => d.hydrated_datasets,
      response.data
    );
  }

  public async loadProjectDatasets(
    projectId: string
  ): Promise<Dataset.Dataset[]> {
    const response = await this.get({
      url: '/v1/modeldb/hydratedData/getHydratedDatasetsByProjectId',
      config: { params: { project_id: projectId } },
    });
    return (response.data.hydrated_datasets || []).map(
      convertServerHydratedDataset
    );
  }

  public async loadDataset(
    id: string,
    workspaceName: IWorkspace['name']
  ): Promise<Dataset.Dataset> {
    const response = await this.post<any, EntityErrorType>({
      url: '/v1/modeldb/hydratedData/findHydratedDatasets',
      data: { dataset_ids: [id], workspace_name: workspaceName },
      errorConverters: {
        accessDeniedToEntity: ({ status }) => status === 403,
        entityNotFound: ({ status }) => status === 404,
      },
    });
    if (!response.data.hydrated_datasets) {
      throw new HttpError<EntityErrorType>({
        status: 404,
        type: 'entityNotFound',
      });
    }
    return convertServerHydratedDataset(response.data.hydrated_datasets[0]);
  }

  public async deleteDataset(id: string): Promise<void> {
    await this.delete({
      url: '/v1/modeldb/dataset/deleteDataset',
      config: { data: { id } },
    });
  }

  public async deleteDatasets(ids: string[]): Promise<void> {
    await this.delete({
      url: '/v1/modeldb/dataset/deleteDatasets',
      config: { data: { ids } },
    });
  }
}

const convertServerHydratedDataset = (
  server: Record<string, any>
): Dataset.Dataset => {
  const { dataset: serverDataset } = server;

  return {
    ...convertServerEntityWithLoggedDates(serverDataset),
    shortWorkspace: convertServerShortWorkspaceToClient(serverDataset),
    description: serverDataset.description || '',
    id: serverDataset.id,
    name: serverDataset.name || '',
    tags: serverDataset.tags || [],
    attributes: serverDataset.attributes || [],
    type: (() => {
      switch (serverDataset.dataset_type) {
        case 0:
        case 'RAW':
          return 'raw';
        case 1:
        case 'PATH':
          return 'path';
        case 2:
        case 'QUERY':
          return 'query';
        default:
          return 'raw';
      }
    })(),
  };
};

export type IServerDatasetsResponse = IServerPaginatedResponse<
  'hydrated_datasets',
  Record<string, any>
>;
