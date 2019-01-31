import { MetaData } from 'models/IMetaData';
import Project from '../models/Project';
import { IDataService } from './IDataService';
import Auth0AuthenticationService from './auth/Auth0AuthenticationService';
import { IAuthenticationService } from './auth/IAuthenticationService';
import ISearchAndFilterService from './ISearchAndFilterService';
import MockDataService from './MockDataService';
import MockSearchAndFiltersService from './MockSearchAndFiltersService';

export default class ServiceFactory {
  public static getDataService(): IDataService {
    return new MockDataService();
  }

  public static getAuthenticationService(): IAuthenticationService {
    return new Auth0AuthenticationService();
  }
  public static getSearchAndFiltersService<T extends MetaData>(ctx: string): ISearchAndFilterService<T> | null {
    switch (ctx) {
      case Project.name: {
        return ServiceFactory.projSFSvc;
      }
    }

    return null;
  }

  private static projSFSvc: MockSearchAndFiltersService = new MockSearchAndFiltersService();
}
