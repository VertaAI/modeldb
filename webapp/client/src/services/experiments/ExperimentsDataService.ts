import { JsonConvert } from 'json2typescript';
import * as R from 'ramda';

import { IArtifact } from 'core/shared/models/Artifact';
import { IFilterData } from 'core/features/filter/Model';
import { IPagination } from 'core/shared/models/Pagination';
import * as Experiment from 'models/Experiment';
import { convertServerCodeVersion } from 'services/serverModel/CodeVersion/converters';
import { convertServerEntityWithLoggedDates } from 'services/serverModel/Common/converters';
import * as EntityAlreadyExistError from '../shared/EntityAlreadyExistError';

import { BaseDataService } from 'core/services/BaseDataService';
import makeLoadExperimentsRequest from './responseRequest/makeLoadExperimentsRequest';
import { ILoadExperimentsResponse } from './types';

export default class ExperimentsDataService extends BaseDataService {
  constructor() {
    super();
  }

  public async createExperiment(
    projectId: string,
    settings: Experiment.IExperimentCreationSettings
  ): Promise<Experiment.default> {
    const serverExperimentSettings = (() => {
      const requestFields: {
        [K in keyof Required<Experiment.IExperimentCreationSettings>]: [
          string,
          any
        ]
      } = {
        name: ['name', settings.name],
        description: ['description', settings.description],
        tags: ['tags', settings.tags],
      };
      return R.fromPairs(R.values(requestFields));
    })();

    const response = await this.post<
      any,
      EntityAlreadyExistError.EntityAlreadyExistsErrorType
    >({
      url: `/v1/modeldb/experiment/createExperiment`,
      data: { ...serverExperimentSettings, project_id: projectId },
      errorConverters: {
        entityAlreadyExists: errorResponse => errorResponse.status === 409,
      },
    });
    return this.loadExperiment(
      response.data.experiment.projectId,
      response.data.experiment.id
    );
  }

  public async loadExperiment(projectId: string, experimentId: string) {
    const response = await this.post({
      url: '/v1/modeldb/hydratedData/findHydratedExperiments',
      data: { project_id: projectId, experiment_ids: [experimentId] },
    });
    return this.convertServerExperiments(response.data)[0];
  }

  public async loadExperiments(
    projectId: string,
    filters: IFilterData[],
    pagination: IPagination
  ): Promise<ILoadExperimentsResponse> {
    const request = await makeLoadExperimentsRequest(
      projectId,
      filters,
      pagination
    );
    const response = await this.post({
      url: '/v1/modeldb/hydratedData/findHydratedExperiments',
      data: request,
    });
    const res: ILoadExperimentsResponse = {
      experiments: this.convertServerExperiments(response.data),
      totalCount: response.data.total_records,
    };
    return res;
  }

  public async deleteExperiment(id: string): Promise<void> {
    await this.delete({
      url: '/v1/modeldb/experiment/deleteExperiment',
      config: { data: { id } },
    });
  }

  public async deleteExperiments(ids: string[]): Promise<void> {
    await this.delete({
      url: '/v1/modeldb/experiment/deleteExperiments',
      config: { data: { ids } },
    });
  }

  public async loadArtifactUrl(
    experimentId: string,
    artifact: IArtifact
  ): Promise<string> {
    const response = await this.post({
      url: '/v1/modeldb/experiment/getUrlForArtifact',
      data: {
        id: experimentId,
        key: artifact.key,
        artifact_type: artifact.type,
        method: 'GET',
      },
    });
    return response.data.url;
  }

  private convertServerExperiments(data: any): Experiment.default[] {
    if (!data || !data.hydrated_experiments) {
      return [];
    }

    return data.hydrated_experiments.map((serverHydratedExperiment: any) => {
      const jsonConvert = new JsonConvert();
      const experiment = jsonConvert.deserializeObject(
        serverHydratedExperiment.experiment,
        Experiment.default
      );

      const dates = convertServerEntityWithLoggedDates(
        serverHydratedExperiment.experiment
      );
      experiment.dateUpdated = dates.dateUpdated;
      experiment.dateCreated = dates.dateCreated;

      experiment.codeVersion = convertServerCodeVersion(
        serverHydratedExperiment.experiment.code_version_snapshot
      );

      return experiment;
    });
  }
}
