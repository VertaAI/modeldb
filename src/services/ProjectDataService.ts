import { IFilterData } from 'models/Filters';
import Project, { UserAccess } from '../models/Project';
import User from '../models/User';
import { PROJECTS_LIST } from './ApiEndpoints';
import { IProjectDataService } from './IApiDataService';
import { projectsMock } from './mocks/projectsMock';
import ServiceFactory from './ServiceFactory';

export default class ProjectDataService implements IProjectDataService {
  private projects: Project[];

  constructor() {
    this.projects = [];
  }

  public getProjects(filter?: IFilterData[]): Promise<Project[]> {
    return new Promise<Project[]>((resolve, reject) => {
      if (process.env.REACT_APP_USE_API_DATA.toString() === 'false') {
        projectsMock.forEach((element: any) => {
          const author = new User(element.owner, 'Manasi.Vartak@verta.ai');
          author.name = 'Manasi Vartak';
          const proj = new Project(element.id, element.name, author);
          proj.Id = element.id || '';
          proj.Description = element.description || '';
          proj.Name = element.name || '';
          proj.Tags = element.tags || '';
          proj.DateCreated = new Date(Number(element.date_created));
          proj.DateUpdated = new Date(Number(element.date_updated));

          for (let index = 0; index < Math.round(Math.random() * 10); index++) {
            const user = new User(element.owner, 'Manasi.Vartak@verta.ai');
            const rand = Math.floor(Math.random() * 2) + 1;
            user.name = `Collaborator ${rand === 2 ? 'Read' : 'Write'}`;
            proj.Collaborators.set(user, rand);
          }

          this.projects.push(proj);
        });

        if (filter !== undefined && filter.length > 0) {
          let result: Project[] = this.projects;
          for (const f of filter) {
            if (f.name === 'Name') {
              result = result.filter(item => item.Name.toLowerCase().indexOf(f.value.toString().toLowerCase()) !== -1);
            }

            if (f.name === 'Tag') {
              result = result.filter(item => item.Tags.findIndex(tag => tag.toLowerCase() === f.value.toString().toLowerCase()) !== -1);
            }

            if (f.name === 'Description') {
              result = result.filter(item => item.Description.toLowerCase().indexOf(f.value.toString().toLowerCase()) !== -1);
            }
          }
          resolve(result);
        } else {
          resolve(this.projects);
        }
      } else {
        const authenticationService = ServiceFactory.getAuthenticationService();
        const url = PROJECTS_LIST.endpoint;
        fetch(url, {
          headers: {
            'Grpc-Metadata-bearer_access_token': authenticationService.accessToken,
            'Grpc-Metadata-source': 'WebApp'
          },
          method: PROJECTS_LIST.method
        })
          .then(res => {
            if (!res.ok) {
              reject(res.statusText);
            }
            return res.json();
          })
          .then(res => {
            if (res.projects === undefined) {
              this.projects.push();
            } else {
              res.projects.forEach((element: any) => {
                const proj = new Project(element.id, element.name, new User(element.owner, 'Manasi.Vartak@verta.ai'));
                proj.Author.name = 'Manasi Vartak';
                proj.Description = element.description || '';
                proj.Tags = element.tags || '';
                proj.DateCreated = new Date(Number(element.date_created));
                proj.DateUpdated = new Date(Number(element.date_updated));
                this.projects.push(proj);
              });

              if (filter !== undefined && filter.length > 0) {
                let result: Project[] = this.projects;
                for (const f of filter) {
                  if (f.name === 'Name') {
                    result = result.filter(item => item.Name.toLowerCase().indexOf(f.value.toString().toLowerCase()) !== -1);
                  }

                  if (f.name === 'Tag') {
                    result = result.filter(
                      item => item.Tags.findIndex(tag => tag.toLowerCase() === f.value.toString().toLowerCase()) !== -1
                    );
                  }

                  if (f.name === 'Description') {
                    result = result.filter(item => item.Description.toLowerCase().indexOf(f.value.toString().toLowerCase()) !== -1);
                  }
                }
                resolve(result);
              } else {
                resolve(this.projects);
              }
            }
          });
      }
    });
  }

  public mapProjectAuthors(): Promise<Project[]> {
    return new Promise<Project[]>((resolve, reject) => {
      // implement mapping for author if any
      resolve(this.projects);
    });
  }
}
