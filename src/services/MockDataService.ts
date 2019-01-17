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
    imdbProj.Description = 'Building model to predict rating for movies from IMDB';
    imdbProj.Name = 'IMDB_exploratory';
    imdbProj.Models.push(model1);

    const hpProj = new Project();
    hpProj.Author = 'Oleg Lukinov';
    hpProj.Description = 'Predict housing prices';
    hpProj.Name = 'Housing Prices';
    hpProj.Models.push(model1);
    hpProj.Models.push(model1);
    hpProj.Models.push(model1);
    hpProj.Models.push(model1);
    hpProj.Models.push(model1);

    this.projects.push(imdbProj);
    this.projects.push(hpProj);
  }
  public getProjects(): Project[] {
    return this.projects;
  }
}
