import { IFilterData } from 'components/FilterSelect/FilterSelect';
import { Model } from '../models/Model';
import Project from '../models/Project';

export interface IDataService {
  getProjects(filter?: IFilterData[]): Promise<Project[]>;
  getProject(id: string): Promise<Project>;
  getModel(id: string): Promise<Model>;
}
