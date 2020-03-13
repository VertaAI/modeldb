import { EntityWithTags } from 'core/shared/models/TagsCRUD';

import { exhaustiveCheck } from 'core/shared/utils/exhaustiveCheck';
import { BaseDataService } from 'core/services/BaseDataService';

export default class TagActionDataService extends BaseDataService {
  constructor() {
    super();
  }

  public addTag(
    id: string,
    tags: string[],
    entityType: EntityWithTags
  ): Promise<string[]> {
    switch (entityType) {
      case 'experimentRun':
        return this.post({
          url: '/v1/modeldb/experiment-run/addExperimentRunTags',
          data: { id, tags },
        }).then(res => res.data.experiment_run.tags || []);
      case 'project': {
        return this.post({
          url: '/v1/modeldb/project/addProjectTags',
          data: { id, tags },
        }).then(res => res.data.project.tags || []);
      }
      case 'experiment': {
        return this.post({
          url: '/v1/modeldb/experiment/addExperimentTags',
          data: { id, tags },
        }).then(res => res.data.experiment.tags || []);
      }
      case 'dataset': {
        return this.post({
          url: '/v1/modeldb/dataset/addDatasetTags',
          data: { id, tags },
        }).then(res => res.data.dataset.tags || []);
      }
      case 'datasetVersion': {
        return this.post({
          url: '/v1/modeldb/dataset-version/addDatasetVersionTags',
          data: {
            id,
            tags,
          },
        }).then(res => res.data.dataset_version.tags || []);
      }
      default:
        return exhaustiveCheck(entityType, '');
    }
  }

  public removeTag(
    id: string,
    tags: string[],
    entityType: EntityWithTags,
    isDeleteAll?: boolean
  ): Promise<string[]> {
    switch (entityType) {
      case 'experimentRun': {
        return this.delete({
          url: '/v1/modeldb/experiment-run/deleteExperimentRunTags',
          config: {
            data: {
              id,
              tags,
              delete_all: isDeleteAll,
            },
          },
        }).then(res => res.data.experiment_run.tags || []);
      }
      case 'project': {
        return this.delete({
          url: '/v1/modeldb/project/deleteProjectTags',
          config: {
            data: {
              id,
              tags,
              delete_all: isDeleteAll,
            },
          },
        }).then(res => res.data.project.tags || []);
      }
      case 'experiment': {
        return this.delete({
          url: '/v1/modeldb/experiment/deleteExperimentTags',
          config: {
            data: {
              id,
              tags,
              delete_all: isDeleteAll,
            },
          },
        }).then(res => res.data.experiment.tags || []);
      }
      case 'dataset': {
        return this.delete({
          url: '/v1/modeldb/dataset/deleteDatasetTags',
          config: {
            data: {
              id,
              tags,
              delete_all: isDeleteAll,
            },
          },
        }).then(res => res.data.dataset.tags || []);
      }
      case 'datasetVersion': {
        return this.delete({
          url: '/v1/modeldb/dataset-version/deleteDatasetVersionTags',
          config: {
            data: {
              id,
              tags,
            },
          },
        }).then(res => res.data.dataset_version.tags || []);
      }
      default:
        return exhaustiveCheck(entityType, '');
    }
  }
}
