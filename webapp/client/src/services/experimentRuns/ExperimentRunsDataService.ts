import { JsonConvert } from 'json2typescript';
import * as R from 'ramda';

import { BaseDataService } from 'core/services/BaseDataService';
import { IArtifact } from 'core/shared/models/Artifact';
import {
  IFilterData,
  PropertyType,
  ComparisonType,
} from 'core/features/filter/Model';
import { IPagination, DataWithPagination } from 'core/shared/models/Pagination';
import { ISorting } from 'core/shared/models/Sorting';
import * as Comments from 'features/comments';
import { ShortExperiment } from 'models/Experiment';
import ModelRecord, { LoadExperimentRunErrorType } from 'models/ModelRecord';
import { convertServerCodeVersion } from 'services/serverModel/CodeVersion/converters';
import { convertServerEntityWithLoggedDates } from 'services/serverModel/Common/converters';

import makeLoadExperimentRunsRequest from './responseRequest/makeLoadExperimentRunsRequest';
import {
  ILoadExperimentRunsResult,
  ILoadModelRecordResult,
  ILazyLoadChartData,
} from './types';

export const chartsPageSettings = {
  pageSize: 50,
  datapointLimit: 500,
};

export default class ExperimentRunsDataService extends BaseDataService {
  constructor() {
    super();
  }

  public async loadExperimentRuns(
    projectId: string,
    filters: IFilterData[] = [],
    pagination: IPagination,
    sorting: ISorting | null
  ): Promise<ILoadExperimentRunsResult> {
    const request = await makeLoadExperimentRunsRequest(
      projectId,
      filters,
      pagination,
      sorting
    );
    const response = await this.post({
      url: '/v1/modeldb/hydratedData/findHydratedExperimentRuns',
      data: request,
    });
    const res: ILoadExperimentRunsResult = {
      data: this.convertExperimentRuns(response.data),
      totalCount: Number(response.data.total_records || 0),
    };
    return res;
  }

  public async loadExperimentRunsByIds(
    projectId: string,
    experimentRunsIds: string[]
  ): Promise<ModelRecord[]> {
    if (experimentRunsIds.length === 0) {
      return Promise.resolve([]);
    }
    const response = await this.post({
      url: '/v1/modeldb/hydratedData/findHydratedExperimentRuns',
      data: {
        project_id: projectId,
        experiment_run_ids: experimentRunsIds,
      },
    });
    return this.convertExperimentRuns(response.data).map(
      ({ experimentRun }) => experimentRun
    );
  }

  public async lazyLoadChartData(
    projectId: string,
    filters: IFilterData[] = []
  ): Promise<ILazyLoadChartData> {
    const sorting = null;
    const paginationInitialLoad: IPagination = {
      currentPage: 0,
      pageSize: chartsPageSettings.pageSize,
      totalCount: 0,
    };

    return makeLoadExperimentRunsRequest(
      projectId,
      filters,
      paginationInitialLoad,
      sorting
    ).then(request => {
      let totalCount = 0;
      return this.post({
        url: '/v1/modeldb/hydratedData/findHydratedExperimentRuns',
        data: request,
      })
        .then(serverResponse => {
          totalCount = serverResponse.data.total_records;
          return this.convertExperimentRuns(serverResponse.data);
        })
        .then(response => {
          const res: ILazyLoadChartData = {
            lazyChartData: response.map(({ experimentRun }) => experimentRun),
            totalCount,
          };
          return res;
        });
    });
  }

  public async loadModelRecord(
    modelId: string
  ): Promise<ILoadModelRecordResult> {
    const response = await this.get<any, LoadExperimentRunErrorType>({
      url: '/v1/modeldb/hydratedData/getHydratedExperimentRunById',
      config: { params: { id: modelId } },
      errorConverters: {
        accessDeniedToEntity: ({ status }) => status === 403,
        entityNotFound: ({ status }) => status === 404,
      },
    });

    const hydrated_experiment_run = response.data.hydrated_experiment_run;
    const jsonConvert = new JsonConvert();
    const modelRecord = jsonConvert.deserializeObject(
      hydrated_experiment_run.experiment_run,
      ModelRecord
    );
    modelRecord.shortExperiment = (() => {
      const shortExperiment = jsonConvert.deserializeObject(
        hydrated_experiment_run.experiment,
        ShortExperiment
      ) as ShortExperiment;
      shortExperiment.id = modelRecord.experimentId;
      return shortExperiment;
    })();
    modelRecord.codeVersion = convertServerCodeVersion(
      hydrated_experiment_run.experiment_run.code_version_snapshot
    );

    const dates = convertServerEntityWithLoggedDates(
      hydrated_experiment_run.experiment_run
    );
    modelRecord.dateCreated = dates.dateCreated;
    modelRecord.dateUpdated = dates.dateUpdated;

    const result: ILoadModelRecordResult = {
      experimentRun: modelRecord,
      comments: (hydrated_experiment_run.comments || []).map((comment: any) =>
        Comments.convertServerComment(comment)
      ),
    };
    return result;
  }

  public async deleteExperimentRun(id: string): Promise<void> {
    await this.delete({
      url: '/v1/modeldb/experiment-run/deleteExperimentRun',
      config: { data: { id } },
    });
  }

  public async deleteExperimentRuns(ids: string[]): Promise<void> {
    await this.delete({
      url: '/v1/modeldb/experiment-run/deleteExperimentRuns',
      config: { data: { ids } },
    });
  }

  public async loadArtifactUrl(
    experimentRunId: string,
    artifact: IArtifact
  ): Promise<string> {
    const response = await this.post({
      url: '/v1/modeldb/experiment-run/getUrlForArtifact',
      data: {
        id: experimentRunId,
        key: artifact.key,
        artifact_type: artifact.type,
        method: 'GET',
      },
    });
    return response.data.url;
  }

  public async deleteArtifact(
    experimentRunId: string,
    artifactKey: string
  ): Promise<void> {
    await this.delete({
      url: '/v1/modeldb/experiment-run/deleteArtifact',
      config: {
        data: {
          id: experimentRunId,
          key: artifactKey,
        },
      },
    });
  }

  public async loadExperimentRunsByDatasetVersionId(
    datasetVersionId: string
  ): Promise<DataWithPagination<ModelRecord>> {
    const {
      data: { projects: serverProjects },
    } = await this.get<{ projects: Array<{ id: string }> }>({
      url: `/v1/modeldb/project/getProjects`,
    });
    const experimentRunsByProjects = await Promise.all(
      (serverProjects || []).map(({ id }) =>
        this.loadExperimentRuns(
          id,
          [
            {
              type: PropertyType.METRIC,
              id: '-1',
              comparisonType: ComparisonType.EQUALS,
              name: 'datasets.linked_artifact_id',
              value: datasetVersionId as any,
            },
          ],
          { currentPage: 0, pageSize: 1000, totalCount: 0 },
          null
        )
      )
    );
    const res: DataWithPagination<ModelRecord> = {
      data: R.chain(
        projectExperimentRuns => projectExperimentRuns.data,
        experimentRunsByProjects
      ).map(({ experimentRun }) => experimentRun),
      totalCount: experimentRunsByProjects
        .map(({ totalCount }) => totalCount)
        .reduce(R.add),
    };
    return res;
  }

  private convertExperimentRuns(
    data: any
  ): Array<{
    experimentRun: ModelRecord;
    comments: Comments.Model.IComment[];
  }> {
    if (!data.hydrated_experiment_runs) {
      return [];
    }

    const jsonConvert = new JsonConvert();
    const experimentRunsWithComments: Array<{
      experimentRun: ModelRecord;
      comments: Comments.Model.IComment[];
    }> = data.hydrated_experiment_runs.map((serverData: any) => {
      const experimentRun = jsonConvert.deserializeObject(
        serverData.experiment_run,
        ModelRecord
      ) as ModelRecord;
      experimentRun.shortExperiment = (() => {
        const shortExperiment = jsonConvert.deserializeObject(
          serverData.experiment,
          ShortExperiment
        ) as ShortExperiment;
        shortExperiment.id = experimentRun.experimentId;
        return shortExperiment;
      })();
      experimentRun.codeVersion = convertServerCodeVersion(
        serverData.experiment_run.code_version_snapshot
      );

      const dates = convertServerEntityWithLoggedDates(
        serverData.experiment_run
      );
      experimentRun.dateCreated = dates.dateCreated;
      experimentRun.dateUpdated = dates.dateUpdated;

      return {
        experimentRun,
        comments: (serverData.comments || []).map((comment: any) =>
          Comments.convertServerComment(comment)
        ),
      };
    });

    return experimentRunsWithComments;
  }
}
