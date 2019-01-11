import { Store } from 'redux';
import { Model, ModelType } from '../models/Model';
import Project from '../models/Project';
import { IDataService } from './IDataService';

export default class MockDataService implements IDataService {
  private projects: Project[];

  constructor(store: Store) {
    this.projects = [];

    const model1 = new Model();
    model1.type = ModelType.LinearRegression;

    const imdbProj = new Project();
    imdbProj.Author = 'Anton Vasin';
    imdbProj.Description = 'Test project for IMDB';
    imdbProj.Name = 'IMDB_exploratory';
    imdbProj.Models.push(model1);

    this.projects.push(imdbProj);
  }
  public getProjects(): Project[] {
    return this.projects;
  }
}
