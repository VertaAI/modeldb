
import Project from '../models/Project';

export interface IDataService {
  getProjects(): Project[];
}
