import { MetaData } from 'models/IMetaData';
import Project from '../models/Project';
import { IAuthenticationService } from './IAuthenticationService';
import { IDataService } from './IDataService';
import ISearchAndFilterService from './ISearchAndFilterService';
import MockAuthenticationService from './MockAuthenticationService';
import MockDataService from './MockDataService';
import MockSearchAndFiltersService from './MockSearchAndFiltersService';

export default class ServiceFactory {
  public static getDataService(): IDataService {
    return new MockDataService();
  }

  public static getAuthenticationService(): IAuthenticationService {
    return new MockAuthenticationService();
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
