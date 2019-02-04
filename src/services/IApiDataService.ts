import ModelRecord from '../models/ModelRecord';
import Project from '../models/Project';

export interface IProjectDataService {
  getProjects(): Promise<Project[]>;
  mapProjectAuthors(): Promise<Project[]>;
}

export interface IExperimentRunsDataService {
  getExperimentRuns(projectId: string): Promise<ModelRecord[]>;
  getModelRecord(modelId: string, storeExperimentRuns?: ModelRecord[]): Promise<ModelRecord>;
}
