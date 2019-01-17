import { string } from 'prop-types';
import { Store } from 'redux';
import { Model, ModelType } from '../models/Model';
import Project from '../models/Project';
import { IDataService } from './IDataService';

export default class MockDataService implements IDataService {
  private projects: Project[];

  constructor() {
    this.projects = [];

    const model1 = new Model();
    model1.Id = '22';
    model1.DataFrameId = '30';
    model1.ModelType = ModelType.LinearRegression;
    model1.ModelMetric = new Map<string, string>([['rmse', '0.881']]);
    model1.Timestamp = new Date();

    const imdbProj = new Project();
    imdbProj.Id = '1';
    imdbProj.Author = 'Anton Vasin';
    imdbProj.Description = 'Building model to predict rating for movies from IMDB';
    imdbProj.Name = 'IMDB_exploratory';
    imdbProj.Models.push(model1);
    imdbProj.Models.push(model1);
    imdbProj.Models.push(model1);
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

  public getProject(id: string): Project {
    return this.projects.find(x => x.Id === id) || new Project();
  }
}
