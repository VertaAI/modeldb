import { IExperimentRunsDataService, IProjectDataService } from './IApiDataService';
import { IAuthenticationService } from './IAuthenticationService';
import ProjectDataService from './ProjectDataService';
import ExperimentRunsDataService from './ExperimentRunsDataService';
import MockAuthenticationService from './MockAuthenticationService';

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
