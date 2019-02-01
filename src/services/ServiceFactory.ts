import { MetaData } from 'models/IMetaData';
import { Model } from '../models/Model';
import Project from '../models/Project';
import Auth0AuthenticationService from './auth/Auth0AuthenticationService';
import { IAuthenticationService } from './auth/IAuthenticationService';
import MockSFModelService from './filter/MockSFModelService';
import MockSFProjectService from './filter/MockSFProjectService';
import MockSFService from './filter/MockSFService';
import { IDataService } from './IDataService';
import ISearchAndFilterService from './ISearchAndFilterService';
import MockDataService from './MockDataService';

export default class ServiceFactory {
  public static getDataService(): IDataService {
    return new MockDataService();
  }

  public static getAuthenticationService(): IAuthenticationService {
    return new Auth0AuthenticationService();
  }
  public static getSearchAndFiltersService<T extends MetaData>(ctx?: string): ISearchAndFilterService<MetaData> | null {
    switch (ctx) {
      case Project.name: {
        return ServiceFactory.projSFSvc;
      }

      case Model.name: {
        return ServiceFactory.modelSFSvc;
      }
    }

    return null;
  }

  private static projSFSvc: MockSFService<Project> = new MockSFProjectService();
  private static modelSFSvc: MockSFService<Model> = new MockSFModelService();
}
