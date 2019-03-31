import axios, { AxiosPromise, AxiosRequestConfig } from 'axios';
import { JsonConvert } from 'json2typescript';

import { IFilterData } from 'models/Filters';
import { Project } from 'models/Project';
import User from 'models/User';

import { BaseDataService } from './BaseDataService';
import { IProjectDataService } from './IProjectDataService';

export class ProjectDataService extends BaseDataService
  implements IProjectDataService {
  public constructor() {
    super();
  }

  public getProjects(filter?: IFilterData[]): AxiosPromise<Project[]> {
    return axios.get<Project[]>(
      '/v1/modeldb/project/getProjects',
      this.responseToProjectConfig(filter)
    );
  }

  public mapProjectAuthors(): AxiosPromise<Project[]> {
    // implement mapping for author if any
    return axios.get<Project[]>(
      '/v1/modeldb/project/getProjects',
      this.responseToProjectConfig()
    );
  }

  private responseToProjectConfig(filters?: IFilterData[]): AxiosRequestConfig {
    return {
      transformResponse: [
        (data: any) => {
          try {
            if (!data || !data.projects) {
              return Array<Project>();
            }

            const jsonConvert = new JsonConvert();
            const projects = jsonConvert.deserializeArray(
              data.projects,
              Project
            ) as Project[];

            for (const project of projects) {
              // remove this after get real User API
              project.Author = new User(
                project.authorId,
                'Manasi.Vartak@verta.ai'
              );
              project.Author.name = 'Manasi Vartak';

              // remove this after get real Collaborators API
              for (
                let index = 0;
                index < Math.round(Math.random() * 10);
                index++
              ) {
                const user = new User(
                  project.authorId,
                  'Manasi.Vartak@verta.ai'
                );
                const rand = Math.floor(Math.random() * 2) + 1;
                user.name = `Collaborator ${rand === 2 ? 'Read' : 'Write'}`;
                project.collaborators.set(user, rand);
              }
            }

            if (filters && filters.length > 0) {
              let result: Project[] = projects;
              for (const filter of filters) {
                if (filter.name === 'Name') {
                  result = result.filter(item =>
                    item.name
                      .toLowerCase()
                      .includes(filter.value.toString().toLowerCase())
                  );
                }

                if (filter.name === 'Tag') {
                  result = result.filter(
                    item =>
                      item.tags.findIndex(
                        tag =>
                          tag.localeCompare(
                            filter.value.toString(),
                            undefined,
                            { sensitivity: 'accent' }
                          ) === 0
                      ) !== -1
                  );
                }

                if (filter.name === 'Description') {
                  result = result.filter(
                    item =>
                      item.description.localeCompare(
                        filter.value.toString(),
                        undefined,
                        { sensitivity: 'accent' }
                      ) === 0
                  );
                }
              }

              return result;
            }
            return projects;
          } catch (error) {
            //console.log(error);
            return data;
          }
        },
      ],
    };
  }
}
