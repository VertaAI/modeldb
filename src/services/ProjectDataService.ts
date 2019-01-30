import Project from '../models/Project';
import { IProjectDataService } from './IApiDataService';

export default class ProjectDataService implements IProjectDataService {
  private projects: Project[];

  constructor() {
    this.projects = [];
  }

  public getProjects(allProjects: any[]): Promise<Project[]> {
    return new Promise<Project[]>((resolve, reject) => {
      fetch('http://localhost:8080/v1/example/getProjects', { method: 'post' })
        .then(res => {
          if (!res.ok) {
            throw Error(res.statusText);
          }
          return res.json();
        })
        .then(res => {
          res.projects.forEach((element: any) => {
            const proj = new Project();
            proj.Id = element.id;
            proj.Author = element.author;
            proj.Description = element.description || '';
            proj.Name = element.name;
            proj.Tags = element.tags || '';
            proj.CreationDate = new Date(Number(element.date_created));
            proj.UpdatedDate = new Date(Number(element.date_updated));
            this.projects.push(proj);
          });
          resolve(this.projects);
        });
    });
  }

  public mapProjectAuthors(): Promise<Project[]> {
    return new Promise<Project[]>((resolve, reject) => {
      // implement mapping for author if any
      resolve(this.projects);
    });
  }
}
