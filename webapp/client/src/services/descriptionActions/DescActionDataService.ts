import { EntityWithDescription } from 'core/shared/models/Description';
import { exhaustiveCheck } from 'core/shared/utils/exhaustiveCheck';

import { BaseDataService } from 'core/services/BaseDataService';

export default class DescActionDataService extends BaseDataService {
  constructor() {
    super();
  }

  public addOrEditDescription(
    id: string,
    description: string,
    entityType: EntityWithDescription
  ): Promise<string> {
    switch (entityType) {
      case 'experimentRun': {
        return this.post({
          url: '/v1/modeldb/experiment-run/updateExperimentRunDescription',
          data: {
            id,
            description,
          },
        }).then(res => res.data.experiment_run.description || '');
      }
      case 'project': {
        return this.post({
          url: '/v1/modeldb/project/updateProjectDescription',
          data: {
            id,
            description,
          },
        }).then(res => res.data.project.description || '');
      }
      case 'experiment': {
        return this.post({
          url: '/v1/modeldb/experiment/updateExperimentNameOrDescription',
          data: {
            id,
            description,
          },
        }).then(res => res.data.experiment.description || '');
      }
      case 'dataset': {
        return this.post({
          url: '/v1/modeldb/dataset/updateDatasetDescription',
          data: {
            id,
            description,
          },
        }).then(res => res.data.dataset.description || '');
      }
      case 'datasetVersion': {
        return this.post({
          url: '/v1/modeldb/dataset-version/updateDatasetVersionDescription',
          data: {
            id,
            description,
          },
        }).then(res => res.data.dataset_version.description || '');
      }
      default:
        return exhaustiveCheck(entityType, '');
    }
  }
}
