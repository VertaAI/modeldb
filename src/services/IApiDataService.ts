import Project from '../models/Project';
import ModelRecord from '../models/ModelRecord';

export interface IProjectDataService {
  getProjects(): Promise<Project[]>;
  mapProjectAuthors(): Promise<Project[]>;
}

export interface IExperimentRunsDataService {
  getExperimentRuns(id: string): Promise<ModelRecord[]>;
}
