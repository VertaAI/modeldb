import ExperimentRunsDataService from './ExperimentRunsDataService';
import { IExperimentRunsDataService, IProjectDataService } from './IApiDataService';
import { IAuthenticationService } from './IAuthenticationService';
import MockAuthenticationService from './MockAuthenticationService';
import ProjectDataService from './ProjectDataService';

export default class ServiceFactory {
  public static getProjectsService(): IProjectDataService {
    return new ProjectDataService();
  }

  public static getExperimentRunsService(): IExperimentRunsDataService {
    return new ExperimentRunsDataService();
  }

  public static getAuthenticationService(): IAuthenticationService {
    return new MockAuthenticationService();
  }
}
