
import Project from '../models/Project';

export default interface IDataService {
  getProjects(): Project[];
}
