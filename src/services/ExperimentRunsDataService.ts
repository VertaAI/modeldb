import { Artifact, ArtifactKey } from '../models/Artifact';
import { ComparisonType, IFilterData, PropertyType } from '../models/Filters';
import { Hyperparameter, IHyperparameter } from '../models/HyperParameters';
import { IMetric, Metric, MetricKey } from '../models/Metrics';
import ModelRecord from '../models/ModelRecord';
import { EXPERIMENT_RUNS } from './ApiEndpoints';
import { IExperimentRunsDataService } from './IApiDataService';
import { expRunsMocks } from './mocks/expRunsMock';
import ServiceFactory from './ServiceFactory';

type Not<T> = [T] extends [never] ? unknown : never;
type Extractable<T, U> = Not<U extends any ? Not<T extends U ? unknown : never> : never>;
function asEnum<E extends Record<keyof E, string | number>, K extends string | number>(
  e: E,
  k: K & Extractable<E[keyof E], K>
): Extract<E[keyof E], K> {
  // runtime guard, shouldn't need it at compiler time
  if (Object.values(e).indexOf(k) < 0) throw new Error(`Expected one of ${Object.values(e).join(', ')}`);
  return k as any; // assertion
}

export default class ExperimentRunsDataService implements IExperimentRunsDataService {
  private experimentRuns: ModelRecord[];

  constructor() {
    this.experimentRuns = [];
  }

  public getExperimentRuns(projectId: string, filters?: IFilterData[]): Promise<ModelRecord[]> {
    return new Promise<ModelRecord[]>((resolve, reject) => {
      if (process.env.REACT_APP_USE_API_DATA.toString() === 'false') {
        expRunsMocks.forEach((element: any) => {
          const modelRecord = new ModelRecord();
          modelRecord.Id = element.id || '';
          modelRecord.ProjectId = element.project_id;
          modelRecord.ExperimentId = element.experiment_id;
          modelRecord.Tags = element.tags || '';
          modelRecord.Name = element.name || '';
          modelRecord.CodeVersion = element.code_version || '';

          element.metrics.forEach((metric: Metric) => {
            modelRecord.Metrics.push(new Metric(metric.key, metric.value));
          });

          element.hyperparameters.forEach((hyperParameter: Hyperparameter) => {
            modelRecord.Hyperparameters.push(new Hyperparameter(hyperParameter.key, hyperParameter.value));
          });

          element.artifacts.forEach((artifact: Artifact) => {
            modelRecord.Artifacts.push(new Artifact(asEnum(ArtifactKey, artifact.key), artifact.path));
          });

          this.experimentRuns.push(modelRecord);
        });

        if (filters !== undefined && filters.length > 0) {
          this.experimentRuns = this.experimentRuns.filter(model => this.checkExperimantRun(model, filters));
        }
        resolve(this.experimentRuns);
      } else {
        const authenticationService = ServiceFactory.getAuthenticationService();
        const url = `${EXPERIMENT_RUNS.endpoint}?project_id=${projectId}`;
        fetch(url, {
          headers: {
            'Grpc-Metadata-bearer_access_token': authenticationService.accessToken,
            'Grpc-Metadata-source': 'WebApp'
          },
          method: EXPERIMENT_RUNS.method
        })
          .then(res => {
            if (!res.ok) {
              reject(res.statusText);
            }
            return res.json();
          })
          .then(res => {
            if (res.experiment_runs === undefined) {
              const emptyModelRecord = new ModelRecord();
              this.experimentRuns.push(emptyModelRecord);
            } else {
              res.experiment_runs.forEach((element: any) => {
                const modelRecord = new ModelRecord();
                modelRecord.Id = element.id || '';
                modelRecord.ProjectId = element.project_id;
                modelRecord.ExperimentId = element.experiment_id;
                modelRecord.Tags = element.tags || '';
                modelRecord.Name = element.name || '';
                modelRecord.CodeVersion = element.code_version || '';

                if (element.metrics !== undefined) {
                  element.metrics.forEach((metric: Metric) => {
                    modelRecord.Metrics.push(new Metric(metric.key, metric.value));
                  });
                }

                if (element.hyperparameters !== undefined) {
                  element.hyperparameters.forEach((hyperParameter: Hyperparameter) => {
                    modelRecord.Hyperparameters.push(new Hyperparameter(hyperParameter.key, hyperParameter.value));
                  });
                }

                if (element.artifacts !== undefined) {
                  element.artifacts.forEach((artifact: Artifact) => {
                    modelRecord.Artifacts.push(new Artifact(asEnum(ArtifactKey, artifact.key), artifact.path));
                  });
                }
                this.experimentRuns.push(modelRecord);
              });
            }
            resolve(this.experimentRuns);
          });
      }
    });
  }

  public getModelRecord(modelId: string, storeExperimentRuns: ModelRecord[]): Promise<ModelRecord> {
    return new Promise<ModelRecord>((resolve, reject) => {
      let modelRecord;
      storeExperimentRuns.forEach(model => {
        if (model.Id === modelId) {
          modelRecord = model;
        }
      });
      resolve(modelRecord);
    });
  }

  private checkExperimantRun(modelRecord: ModelRecord, filters: IFilterData[]) {
    for (const filter of filters) {
      const propName: string = filter.name.toLocaleLowerCase();
      const filterValue = filter.value;

      if (propName === 'tag') {
        if (modelRecord.Tags.includes(filter.value.toString())) {
          return true;
        }
      }

      if (propName === 'name') {
        if (modelRecord.Name === filter.value.toString()) {
          return true;
        }
      }

      if (propName === 'id') {
        if (modelRecord.Id === filter.value.toString()) {
          return true;
        }
      }

      if (propName === 'ProjectId') {
        if (modelRecord.ProjectId === filter.value.toString()) {
          return true;
        }
      }

      if (filter.type === PropertyType.METRIC) {
        let val;
        const m: IMetric | undefined = modelRecord.Metrics.find(metric => metric.key === filter.name);
        if (m !== undefined) {
          val = m.value;
        }

        const h: IHyperparameter | undefined = modelRecord.Hyperparameters.find(metric => metric.key === filter.name);
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
