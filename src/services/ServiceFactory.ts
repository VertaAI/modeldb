import Auth0AuthenticationService from './auth/Auth0AuthenticationService';
import { IAuthenticationService } from './auth/IAuthenticationService';
import CollaboratorsService from './CollaboratorsService';
import ExperimentRunsDataService from './ExperimentRunsDataService';
import MockSFService from './filter/MockSFService';
import { ICollaboratorsService } from './ICollaboratorsService';
import { IExperimentRunsDataService } from './IExperimentRunsDataService';
import { IProjectDataService } from './IProjectDataService';
import ISearchAndFilterService from './ISearchAndFilterService';
import { MockExperimentRunsDataService } from './mocks/MockExperimentRunsDataService';
import { MockProjectDataService } from './mocks/MockProjectDataService';
import { ProjectDataService } from './ProjectDataService';
import { IDeployService } from './IDeployService';
import { DeployService } from './DeployService';

export default class ServiceFactory {
  public static getProjectsService(): IProjectDataService {
    if (JSON.parse(process.env.REACT_APP_USE_API_DATA)) {
      return new ProjectDataService();
    }
    return new MockProjectDataService();
  }

  public static getExperimentRunsService(): IExperimentRunsDataService {
    if (true) {
      return new ExperimentRunsDataService();
    }
    return new MockExperimentRunsDataService();
  }

  public static getAuthenticationService(): IAuthenticationService {
    return new Auth0AuthenticationService();
  }

  public static getSearchAndFiltersService(): ISearchAndFilterService | null {
    return new MockSFService();
  }

  public static getCollaboratorsService(): ICollaboratorsService {
    return new CollaboratorsService();
  }

  public static getDeployService(): IDeployService {
    return new DeployService();
  }
}
