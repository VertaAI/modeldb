import axios, { AxiosPromise, AxiosRequestConfig } from 'axios';
import { JsonConvert } from 'json2typescript';

import { ComparisonType, IFilterData, PropertyType } from 'models/Filters';
import { IHyperparameter } from 'models/HyperParameters';
import { IMetric } from 'models/Metrics';
import ModelRecord from 'models/ModelRecord';

import { BaseDataService } from './BaseDataService';
import { IExperimentRunsDataService } from './IExperimentRunsDataService';

export default class ExperimentRunsDataService extends BaseDataService implements IExperimentRunsDataService {
  constructor() {
    super();
  }

  public getExperimentRuns(projectId: string, filters?: IFilterData[]): AxiosPromise<ModelRecord[]> {
    const axiosConfig = this.responseToExperimentRunsConfig(filters);
    axiosConfig.params = { project_id: projectId };
    return axios.get<ModelRecord[]>('/getExperimentRunsInProject', axiosConfig);
  }

  public getModelRecord(modelId: string, storeExperimentRuns: ModelRecord[]): Promise<ModelRecord> {
    return new Promise<ModelRecord>(resolve => {
      let modelRecord;
      storeExperimentRuns.forEach(model => {
        if (model.id === modelId) {
          modelRecord = model;
        }
      });
      resolve(modelRecord);
    });
  }

  private responseToExperimentRunsConfig(filters?: IFilterData[]): AxiosRequestConfig {
    return {
      transformResponse: [
        (data: any) => {
          try {
            if (!data || !data.experiment_runs) {
              return Array<ModelRecord>();
            }

            const jsonConvert = new JsonConvert();
            let experimentRuns = jsonConvert.deserializeArray(data.experiment_runs, ModelRecord) as ModelRecord[];
            if (filters && filters.length > 0) {
              experimentRuns = experimentRuns.filter(model => this.checkExperimentRun(model, filters));
            }

            return experimentRuns;
          } catch (error) {
            console.log(error);
            return data;
          }
        }
      ]
    };
  }

  private checkExperimentRun(modelRecord: ModelRecord, filters: IFilterData[]) {
    for (const filter of filters) {
      const propName: string = filter.name.toLocaleLowerCase();
      const filterValue = filter.value;

      if (propName === 'tag') {
        if (modelRecord.tags.includes(filter.value.toString())) {
          return true;
        }
      }

      if (propName === 'name') {
        if (modelRecord.name === filter.value.toString()) {
          return true;
        }
      }

      if (propName === 'id') {
        if (modelRecord.id === filter.value.toString()) {
          return true;
        }
      }

      if (propName === 'ProjectId') {
        if (modelRecord.projectId === filter.value.toString()) {
          return true;
        }
      }

      if (filter.type === PropertyType.METRIC) {
        let val;
        const m: IMetric | undefined = modelRecord.metrics.find(metric => metric.key === filter.name);
        if (m !== undefined) {
          val = m.value;
        }

        const h: IHyperparameter | undefined = modelRecord.hyperparameters.find(metric => metric.key === filter.name);
        if (h !== undefined) {
          val = h.value;
        }

        if (val === undefined) {
          continue;
        }

        if (filter.comparisonType === ComparisonType.LESS) {
          return filterValue > val;
        }
        if (filter.comparisonType === ComparisonType.MORE) {
          return filterValue < val;
        }
        if (filter.comparisonType === ComparisonType.EQUALS) {
          return filterValue === val;
        }
      }
    }

    return false;
  }
}
