import { IFilterData } from '../models/Filters';
import ModelRecord from '../models/ModelRecord';
import Project from '../models/Project';

export interface IProjectDataService {
  getProjects(filter?: IFilterData[]): Promise<Project[]>;
  mapProjectAuthors(): Promise<Project[]>;
}

export interface IExperimentRunsDataService {
  getExperimentRuns(projectId: string): Promise<ModelRecord[]>;
  getModelRecord(modelId: string, storeExperimentRuns?: ModelRecord[]): Promise<ModelRecord>;
}
