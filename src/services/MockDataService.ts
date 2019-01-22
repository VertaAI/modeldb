import { Artifact, ArtifactKey, ArtifactType } from '../models/Artifact';
import { Model, ModelType } from '../models/Model';
import Project from '../models/Project';
import { IDataService } from './IDataService';

export default class MockDataService implements IDataService {
  private projects: Project[];

  constructor() {
    this.projects = [];

    const model1 = new Model();
    model1.Name = 'test';
    model1.Id = '22';
    model1.ProjectId = '13';
    model1.ExperimentId = '15';
    model1.DataFrameId = '30';
    model1.ModelType = ModelType.LinearRegression;
    model1.Tags = ['tag1', 'tag2'];
    model1.Hyperparameters = new Map<string, string>([['C', '10'], ['solver', 'lbjfs']]);
    model1.ModelMetric = new Map<string, string>([['rmse', '0.881'], ['f1', '0.222']]);
    model1.Artifacts = [
      new Artifact(
        ArtifactKey.InputData,
        'https://upload.wikimedia.org/wikipedia/commons/b/b6/Image_created_with_a_mobile_phone.png',
        ArtifactType.Data
      )
    ];
    model1.DataSets = [
      new Artifact(
        ArtifactKey.Model,
        'https://upload.wikimedia.org/wikipedia/commons/b/b6/Image_created_with_a_mobile_phone.png',
        ArtifactType.Data
      )
    ];

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
    hpProj.Id = '2';
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
  public getModel(id: string): Model {
    let foundModel;

    this.projects.forEach(project => {
      project.Models.forEach(model => {
        if (model.Id === id) {
          foundModel = model;
        }
      });
    });
    return foundModel || new Model();
  }
}
