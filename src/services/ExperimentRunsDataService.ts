import ModelRecord from '../models/ModelRecord';
import { IExperimentRunsDataService } from './IApiDataService';
import { EXPERIMENT_RUNS, MODEL_RECORD } from './ApiEndpoints';
import { MetricKey, Metric } from '../models/Metrics';
import { Hyperparameter } from '../models/HyperParameters';
import { Artifact, ArtifactKey } from '../models/Artifact';

type Not<T> = [T] extends [never] ? unknown : never;
type Extractable<T, U> = Not<U extends any ? Not<T extends U ? unknown : never> : never>;
function asEnum<E extends Record<keyof E, string | number>, K extends string | number>(
  e: E,
  k: K & Extractable<E[keyof E], K>
): Extract<E[keyof E], K> {
  // runtime guard, shouldn't need it at compiler time
  //   if (Object.values(e).indexOf(k) < 0) throw new Error(`Expected one of ${Object.values(e).join(', ')}`);
  return k as any; // assertion
}

export default class ExperimentRunsDataService implements IExperimentRunsDataService {
  private experiment_runs: ModelRecord[];
  private model_record: ModelRecord;

  constructor() {
    this.experiment_runs = [];
    this.model_record = new ModelRecord();
  }

  public getExperimentRuns(project_id: string): Promise<ModelRecord[]> {
    return new Promise<ModelRecord[]>((resolve, reject) => {
      fetch(EXPERIMENT_RUNS.endpoint, { method: EXPERIMENT_RUNS.method, body: EXPERIMENT_RUNS.body(project_id) })
        .then(res => {
          if (!res.ok) {
            throw Error(res.statusText);
          }
          return res.json();
        })
        .then(res => {
          res.experiment_runs.forEach((element: any) => {
            const model_record = new ModelRecord();
            model_record.Id = element.id;
            model_record.ProjectId = element.project_id;
            model_record.ExperimentId = element.experiment_id;
            model_record.Tags = element.tags || '';
            model_record.Name = element.name;
            model_record.CodeVersion = element.code_version || '';

            element.metrics.forEach((metric: Metric) => {
              model_record.Metric.push(new Metric(asEnum(MetricKey, metric.key), metric.value));
            });

            element.hyperparameters.forEach((hyperParameter: Hyperparameter) => {
              model_record.Hyperparameters.push(new Hyperparameter(hyperParameter.key, hyperParameter.value));
            });

            element.artifacts.forEach((artifact: Artifact) => {
              model_record.Artifacts.push(new Artifact(asEnum(ArtifactKey, artifact.key), artifact.path));
            });

            this.experiment_runs.push(model_record);
          });
          resolve(this.experiment_runs);
        });
    });
  }

  public getModelRecord(model_id: string, store_experiment_runs: ModelRecord[]): Promise<ModelRecord> {
    return new Promise<ModelRecord>((resolve, reject) => {
      store_experiment_runs.forEach(model => {
        if (model.Id === model_id) {
          this.model_record = model;
        }
      });
      resolve(this.model_record);
    });
  }
}
