import { AxiosPromise } from 'axios';

import { IFilterData } from '../models/Filters';
import { Project } from '../models/Project';

export interface IProjectDataService {
  getProjects(filter?: IFilterData[]): AxiosPromise<Project[]>;
  mapProjectAuthors(): AxiosPromise<Project[]>;
}
