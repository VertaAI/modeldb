import Auth0AuthenticationService from './auth/Auth0AuthenticationService';
import { IAuthenticationService } from './auth/IAuthenticationService';
import MockAuthenticationService from './auth/MockAuthenticationService';
import CollaboratorsService from './CollaboratorsService';
import { DeployService } from './DeployService';
import ExperimentRunsDataService from './ExperimentRunsDataService';
import MockSFService from './filter/MockSFService';
import { ICollaboratorsService } from './ICollaboratorsService';
import { IDeployService } from './IDeployService';
import { IExperimentRunsDataService } from './IExperimentRunsDataService';
import { IProjectDataService } from './IProjectDataService';
import ISearchAndFilterService from './ISearchAndFilterService';
import MockDeployService from './mocks/MockDeployService';
import { MockExperimentRunsDataService } from './mocks/MockExperimentRunsDataService';
import { MockProjectDataService } from './mocks/MockProjectDataService';
import { ProjectDataService } from './ProjectDataService';

export default class ServiceFactory {
  public static getProjectsService(): IProjectDataService {
    if (JSON.parse(process.env.REACT_APP_USE_API_DATA)) {
      return new ProjectDataService();
    }
    return new MockProjectDataService();
  }

  public static getExperimentRunsService(): IExperimentRunsDataService {
    console.log(JSON.parse(process.env.REACT_APP_USE_API_DATA));
    if (JSON.parse(process.env.REACT_APP_USE_API_DATA)) {
      return new ExperimentRunsDataService();
    }
    return new MockExperimentRunsDataService();
  }

  public static getAuthenticationService(): IAuthenticationService {
    if (JSON.parse(process.env.REACT_APP_USE_API_DATA)) {
      return new Auth0AuthenticationService();
    }
    return new MockAuthenticationService();
  }

  public static getSearchAndFiltersService(): ISearchAndFilterService | null {
    return new MockSFService();
  }

  public static getCollaboratorsService(): ICollaboratorsService {
    return new CollaboratorsService();
  }

  public static getDeployService(): IDeployService {
    if (JSON.parse(process.env.REACT_APP_USE_API_DATA)) {
      return new DeployService();
    }
    return new MockDeployService();
  }
}
