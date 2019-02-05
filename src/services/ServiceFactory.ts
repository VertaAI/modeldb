import { MetaData } from 'models/IMetaData';
import ModelRecord from '../models/ModelRecord';
import Project from '../models/Project';
import Auth0AuthenticationService from './auth/Auth0AuthenticationService';
import { IAuthenticationService } from './auth/IAuthenticationService';
import ExperimentRunsDataService from './ExperimentRunsDataService';
import { IExperimentRunsDataService, IProjectDataService } from './IApiDataService';
import ProjectDataService from './ProjectDataService';

import MockSFModelService from './filter/MockSFModelService';
import MockSFProjectService from './filter/MockSFProjectService';
import MockSFService from './filter/MockSFService';
import ISearchAndFilterService from './ISearchAndFilterService';

export default class ServiceFactory {
  public static getProjectsService(): IProjectDataService {
    return new ProjectDataService();
  }

  public static getExperimentRunsService(): IExperimentRunsDataService {
    return new ExperimentRunsDataService();
  }

  public static getAuthenticationService(): IAuthenticationService {
    return new Auth0AuthenticationService();
  }
  public static getSearchAndFiltersService<T extends MetaData>(ctx?: string): ISearchAndFilterService<MetaData> | null {
    switch (ctx) {
      case Project.name: {
        return ServiceFactory.projSFSvc;
      }

      case ModelRecord.name: {
        return ServiceFactory.modelSFSvc;
      }
    }

    return null;
  }

  private static projSFSvc: MockSFService<Project> = new MockSFProjectService();
  private static modelSFSvc: MockSFService<ModelRecord> = new MockSFModelService();
}
