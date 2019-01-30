import Project from '../models/Project';
import { ModelRecord } from '../models/ModelRecord';

export interface IProjectDataService {
  getProjects(allProjects: object[]): Promise<Project[]>;
  mapProjectAuthors(): Promise<Project[]>;
}

export interface IExperimentsDataService {
  getExperimentRuns(): Promise<ModelRecord[]>;
}
