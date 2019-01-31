import ModelRecord from '../models/ModelRecord';
import { IExperimentRunsDataService } from './IApiDataService';
import { EXPERIMENT_RUNS } from './ApiEndpoints';

export default class ExperimentRunsDataService implements IExperimentRunsDataService {
  private experimentRuns: ModelRecord[];

  constructor() {
    this.experimentRuns = [];
  }

  public getExperimentRuns(id: string): Promise<ModelRecord[]> {
    return new Promise<ModelRecord[]>((resolve, reject) => {
      fetch('http://localhost:8080/v1/example/getExperimentRunsInProject', { method: 'post', body: JSON.stringify({ project_id: id }) })
        .then(res => {
          if (!res.ok) {
            throw Error(res.statusText);
          }
          return res.json();
        })
        .then(res => {
          res.experiment_runs.forEach((element: any) => {
            const model = new ModelRecord();
            model.Id = element.id;
            model.Tags = element.tags || '';
            model.Name = element.name;
            model.CodeVersion = element.codeVersion || '';
            this.experimentRuns.push(model);
          });
          resolve(this.experimentRuns);
        });
    });
  }
}
