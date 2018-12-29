
import Project from '../models/Project';
import IDataService from './IDataService';

export default class MockDataService implements IDataService {

  private projects: Project[];

  constructor() {
    this.projects = [];

  }
  public getProjects(): Project[] {
    return this.projects;
  }
}
