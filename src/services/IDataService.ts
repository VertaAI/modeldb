import { Model } from '../models/Model';
import Project from '../models/Project';

export interface IDataService {
  getProjects(): Project[];
  getProject(id: string): Project;
  getModel(id: string): Model;
}
