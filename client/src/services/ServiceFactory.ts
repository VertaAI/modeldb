import Auth0AuthenticationService from './auth/Auth0AuthenticationService';
import { IAuthenticationService } from './auth/IAuthenticationService';
import MockAuthenticationService from './auth/MockAuthenticationService';
import CollaboratorsService from './collaborators/CollaboratorsService';
import { ICollaboratorsService } from './collaborators/ICollaboratorsService';
import ExperimentRunsDataService from './experimentRuns/ExperimentRunsDataService';
import { IExperimentRunsDataService } from './experimentRuns/IExperimentRunsDataService';
import MockExperimentRunsDataService from './experimentRuns/MockExperimentRunsDataService';
import { ISearchAndFilterService } from './filter/ISearchAndFilterService';
import MockSFService from './filter/MockSFService';
import { IProjectDataService } from './projects/IProjectDataService';
import MockProjectDataService from './projects/MockProjectDataService';
import ProjectDataService from './projects/ProjectDataService';

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

  public static getSearchAndFiltersService(): ISearchAndFilterService {
    return new MockSFService();
  }

  public static getCollaboratorsService(): ICollaboratorsService {
    return new CollaboratorsService();
  }
}
