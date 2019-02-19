import { IFilterData } from 'models/Filters';
import Project, { UserAccess } from '../models/Project';
import User from '../models/User';
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
        const author = new User(this.generateId(), 'Manasi.Vartak@verta.ai');
        author.name = 'Manasi Vartak';
        const proj = new Project(element.id, element.name, author);

        proj.Id = element.id;
        proj.Description = element.description || '';
        proj.Name = element.name;
        proj.Tags = element.tags || '';
        proj.DateCreated = new Date(Number(element.date_created));
        proj.DateUpdated = new Date(Number(element.date_updated));

        for (let index = 0; index < Math.round(Math.random() * 10); index++) {
          const user = new User(this.generateId(), 'Manasi.Vartak@verta.ai');
          const rand = Math.floor(Math.random() * 2) + 1;
          user.name = `Collaborator ${rand === 2 ? 'Read' : 'Write'}`;
          proj.Collaborators.set(user, rand);
        }

        this.projects.push(proj);
      });

      if (filter !== undefined && filter.length > 0) {
        resolve(this.projects.slice(0, 1));
      } else {
        resolve(this.projects);
      }

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

  private generateId() {
    return `_${Math.random()
      .toString(36)
      .substr(2, 9)}`;
  }
}
