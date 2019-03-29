import { AxiosPromise } from 'axios';

import { IFilterData } from 'models/Filters';
import ModelRecord from 'models/ModelRecord';

export interface IExperimentRunsDataService {
  getExperimentRuns(
    projectId: string,
    filters?: IFilterData[]
  ): AxiosPromise<ModelRecord[]>;
  getModelRecord(
    modelId: string,
    storeExperimentRuns?: ModelRecord[]
  ): Promise<ModelRecord>;
}
