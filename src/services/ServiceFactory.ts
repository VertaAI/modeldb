import Auth0AuthenticationService from './auth/Auth0AuthenticationService';
import { IAuthenticationService } from './auth/IAuthenticationService';
import MockAuthenticationService from './auth/MockAuthenticationService';
import { IDataService } from './IDataService';
import MockDataService from './MockDataService';

export default class ServiceFactory {
  public static getDataService(): IDataService {
    return new MockDataService();
  }

  public static getAuthenticationService(): IAuthenticationService {
    return new Auth0AuthenticationService();
  }
}
