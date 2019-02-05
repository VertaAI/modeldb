import { IFilterData } from 'components/FilterSelect/FilterSelect';
import Project from '../models/Project';
import { PROJECTS_LIST } from './ApiEndpoints';
import { IProjectDataService } from './IApiDataService';
import { projectsMock } from './mocks/projectsMock';

export default class ProjectDataService implements IProjectDataService {
  private projects: Project[];

  constructor() {
    this.projects = [];
  }

  public getProjects(filter?: IFilterData[]): Promise<Project[]> {
    return new Promise<Project[]>((resolve, reject) => {
      projectsMock.forEach((element: any) => {
        const proj = new Project();

        proj.Id = element.id;
        proj.Author = element.author;
        proj.Description = element.description || '';
        proj.Name = element.name;
        proj.Tags = element.tags || '';
        proj.DateCreated = new Date(Number(element.date_created));
        proj.DateUpdated = new Date(Number(element.date_updated));
        this.projects.push(proj);
      });
      resolve(this.projects);

      // could be used post API activation
      // fetch(PROJECTS_LIST.endpoint, { method: PROJECTS_LIST.method })
      //   .then(res => {
      //     if (!res.ok) {
      //       reject(res.statusText);
      //     }
      //     return res.json();
      //   })
      //   .then(res => {
      //     console.log(projectsMock);
      //     console.log(res);
      //     res.projects.forEach((element: any) => {
      //       const proj = new Project();

      //       proj.Id = element.id;
      //       proj.Author = element.author;
      //       proj.Description = element.description || '';
      //       proj.Name = element.name;
      //       proj.Tags = element.tags || '';
      //       proj.DateCreated = new Date(Number(element.date_created));
      //       proj.DateUpdated = new Date(Number(element.date_updated));
      //       this.projects.push(proj);
      //     });
      //     resolve(this.projects);
      //   });
    });
  }

  public mapProjectAuthors(): Promise<Project[]> {
    return new Promise<Project[]>((resolve, reject) => {
      // implement mapping for author if any
      resolve(this.projects);
    });
  }
}
