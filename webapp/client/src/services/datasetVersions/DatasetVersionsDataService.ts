import { BaseDataService } from 'core/services/BaseDataService';
import { EntityErrorType } from 'core/shared/models/Common';
import { IFilterData } from 'core/features/filter/Model';
import { DataWithPagination, IPagination } from 'core/shared/models/Pagination';
import {
  IDatasetVersion,
  IPathBasedDatasetVersionInfo,
  IQueryDatasetVersionInfo,
  IRawDatasetVersionInfo,
  DatasetVersionPathLocationTypes,
} from 'models/DatasetVersion';

import makeLoadDatasetVersionsRequest from './responseRequest/makeLoadDatasetVersionsRequest';

export default class DatasetVersionsDataService extends BaseDataService {
  constructor() {
    super();
  }

  public async loadDatasetVersions(
    datasetId: string,
    filters: IFilterData[],
    pagination: IPagination
  ): Promise<DataWithPagination<IDatasetVersion>> {
    const request = await makeLoadDatasetVersionsRequest(
      datasetId,
      filters,
      pagination
    );
    const response = await this.post({
      url: '/v1/modeldb/hydratedData/findHydratedDatasetVersions',
      data: request,
    });
    const res: DataWithPagination<IDatasetVersion> = {
      data: (response.data.hydrated_dataset_versions || []).map(
        convertServerHydratedDatasetVersion
      ),
      totalCount: response.data.total_records
        ? Number(response.data.total_records)
        : 0,
    };
    return res;
  }

  public async loadDatasetVersion(
    datasetVersionId: string,
    datasetId?: string
  ): Promise<IDatasetVersion> {
    const response = await this.post<any, EntityErrorType>({
      url: '/v1/modeldb/hydratedData/findHydratedDatasetVersions',
      data: {
        ...(datasetId ? { dataset_id: datasetId } : {}),
        dataset_version_ids: [datasetVersionId],
      },
      errorConverters: {
        accessDeniedToEntity: ({ status }) => status === 403,
        entityNotFound: ({ status }) => status === 404,
      },
    });
    return convertServerHydratedDatasetVersion(
      response.data.hydrated_dataset_versions[0]
    );
  }

  public async deleteDatasetVersion(id: string): Promise<void> {
    await this.delete({
      url: '/v1/modeldb/dataset-version/deleteDatasetVersion',
      config: { data: { id } },
    });
  }

  public async deleteDatasetVersions(ids: string[]): Promise<void> {
    await this.delete({
      url: '/v1/modeldb/dataset-version/deleteDatasetVersions',
      config: { data: { ids } },
    });
  }
}

const convertServerHydratedDatasetVersion = ({
  dataset_version: serverDatasetVersion,
}: any): IDatasetVersion => {
  const type = (() => {
    if ('raw_dataset_version_info' in serverDatasetVersion) {
      return 'raw';
    }
    if ('path_dataset_version_info' in serverDatasetVersion) {
      return 'path';
    }
    if ('query_dataset_version_info' in serverDatasetVersion) {
      return 'query';
    }
  })();
  return {
    id: serverDatasetVersion.id,
    parentId: serverDatasetVersion.parent_id,
    attributes: serverDatasetVersion.attributes || [],
    datasetId: serverDatasetVersion.dataset_id,
    description: serverDatasetVersion.description || '',
    version: serverDatasetVersion.version,
    tags: serverDatasetVersion.tags || [],
    dateLogged: new Date(Number(serverDatasetVersion.time_logged)),
    dateUpdated: new Date(Number(serverDatasetVersion.time_updated)),
    isPubliclyVisible: serverDatasetVersion.dataset_version_visibility !== 0,
    type,
    info: (() => {
      if (type === 'path') {
        const serverInfo = serverDatasetVersion.path_dataset_version_info;
        const info: IPathBasedDatasetVersionInfo = {
          size:
            serverInfo.size != undefined ? Number(serverInfo.size) : undefined,
          basePath: serverInfo.base_path,
          locationType: (() => {
            switch (serverInfo.location_type) {
              case 'LOCAL_FILE_SYSTEM':
                return DatasetVersionPathLocationTypes.localFileSystem;
              case 'NETWORK_FILE_SYSTEM':
                return DatasetVersionPathLocationTypes.networkFileSystem;
              case 'HADOOP_FILE_SYSTEM':
                return DatasetVersionPathLocationTypes.hadoopFileSystem;
              case 'S3_FILE_SYSTEM':
                return DatasetVersionPathLocationTypes.s3FileSystem;
              default:
                return undefined;
            }
          })(),
          datasetPathInfos: (serverInfo.dataset_part_infos || []).map(
            ({ path, size, checksum, last_modified_at_source }: any) => {
              return {
                path,
                size,
                checkSum: checksum,
                lastModified: last_modified_at_source,
              };
            }
          ),
        };
        return info;
      }
      if (type === 'query') {
        const serverInfo = serverDatasetVersion.query_dataset_version_info;
        const info: IQueryDatasetVersionInfo = {
          dataSourceUri: serverInfo.data_source_uri,
          executionTimestamp: serverInfo.execution_timestamp,
          query: serverInfo.query,
          queryTemplate: serverInfo.query_template,
          queryParameters: (serverInfo.query_parameters || []).map(
            ({ parameter_name, value }: any) => ({
              name: parameter_name,
              value,
            })
          ),
          numRecords: serverInfo.num_records,
        };
        return info;
      }
      const serverInfo = serverDatasetVersion.raw_dataset_version_info;
      const info: IRawDatasetVersionInfo = {
        checkSum: serverInfo.checksum,
        features: serverInfo.features || [],
        objectPath: serverInfo.object_path,
        numRecords: serverInfo.num_records,
        size: serverInfo.size != undefined ? serverInfo.size : undefined,
      };
      return info;
    })(),
  } as IDatasetVersion;
};
