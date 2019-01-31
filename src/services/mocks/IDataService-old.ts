import { Model } from '../../models/Model';
import Project from '../../models/Project';

export interface IDataService {
  getProjects(): Promise<Project[]>;
  getProject(id: string): Promise<Project>;
  // getModel(id: string): Promise<Model>;
}
