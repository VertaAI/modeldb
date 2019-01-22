import { IAuthenticationService } from './IAuthenticationService';
import { IDataService } from './IDataService';
import MockAuthenticationService from './MockAuthenticationService';
import MockDataService from './MockDataService';

export default class ServiceFactory {
  public static getDataService(): IDataService {
    return new MockDataService();
  }

  public static getAuthenticationService(): IAuthenticationService {
    return new MockAuthenticationService();
  }
}
