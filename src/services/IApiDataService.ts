import Project from '../models/Project';
import ModelRecord from '../models/ModelRecord';

export interface IProjectDataService {
  getProjects(): Promise<Project[]>;
  mapProjectAuthors(): Promise<Project[]>;
}

export interface IExperimentRunsDataService {
  getExperimentRuns(project_id: string): Promise<ModelRecord[]>;
  getModelRecord(model_id: string, store_experiment_runs: ModelRecord[]): Promise<ModelRecord>;
}
