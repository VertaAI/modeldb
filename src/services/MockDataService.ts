import { Artifact, ArtifactKey, ArtifactType } from '../models/Artifact';
import { Hyperparameter } from '../models/HyperParameters';
import { Model } from '../models/Model';
import { MetricKey, ModelMetric, ValueType } from '../models/ModelMetric';
import Project from '../models/Project';
import { IDataService } from './IDataService';
import { modelsMocks } from './mocks/modelsMocks';
import { projectsMock } from './mocks/projectsMocks';

type Not<T> = [T] extends [never] ? unknown : never;
type Extractable<T, U> = Not<U extends any ? Not<T extends U ? unknown : never> : never>;
function asEnum<E extends Record<keyof E, string | number>, K extends string | number>(
  e: E,
  k: K & Extractable<E[keyof E], K>
): Extract<E[keyof E], K> {
  // runtime guard, shouldn't need it at compiler time
  if (Object.values(e).indexOf(k) < 0) throw new Error(`Expected one of ${Object.values(e).join(', ')}`);
  return k as any; // assertion
}

export default class MockDataService implements IDataService {
  private projects: Project[];

  constructor() {
    const models: Model[] = [];
    modelsMocks.forEach(element => {
      const model = new Model();
      model.Id = element.id;
      model.Name = element.name;
      model.ProjectId = element.projectId;
      model.ExperimentId = element.experimentId;
      model.Tags = element.tags || [];
      element.hyperparameters.forEach(hyperParameter => {
        model.Hyperparameters.push(new Hyperparameter(hyperParameter.key, hyperParameter.value));
      });
      element.metrics.forEach(metric => {
        model.ModelMetric.push(new ModelMetric(asEnum(MetricKey, metric.key), metric.value, asEnum(ValueType, metric.valueType)));
      });
      element.artifacts.forEach(artifact => {
        model.Artifacts.push(new Artifact(asEnum(ArtifactKey, artifact.key), artifact.path, asEnum(ArtifactType, artifact.artifactType)));
      });
      element.datasets.forEach(dataset => {
        model.DataSets.push(new Artifact(asEnum(ArtifactKey, dataset.key), dataset.path, asEnum(ArtifactType, dataset.artifactType)));
      });
      models.push(model);
    });

    this.projects = [];
    projectsMock.forEach(element => {
      const proj = new Project();
      proj.Id = element.id;
      proj.Author = element.author;
      proj.Description = element.description || '';
      proj.Name = element.name;
      proj.CreationDate = new Date(element.dateCreated);
      proj.UpdatedDate = new Date(element.dateUpdated);

      models.forEach(model => {
        if (model.ProjectId === proj.Id) {
          model.ProjectName = proj.Name;
          proj.Models.push(model);
        }
      });

      this.projects.push(proj);
    });
  }

  public getProjects(): Promise<Project[]> {
    return new Promise<Project[]>((resolve, reject) => {
      resolve(this.projects);
    });
  }

  public getProject(id: string): Promise<Project> {
    return new Promise<Project>((resolve, reject) => {
      const project = this.projects.find(x => x.Id === id);
      project ? resolve(project) : reject('error');
    });
  }

  public getModel(id: string): Promise<Model> {
    return new Promise<Model>((resolve, reject) => {
      let foundModel;

      this.projects.forEach(project => {
        project.Models.forEach(model => {
          if (model.Id === id) {
            foundModel = model;
          }
        });
      });
      foundModel ? resolve(foundModel) : reject();
    });
  }
}
