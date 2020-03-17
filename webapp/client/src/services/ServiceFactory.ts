import axios, { AxiosError } from 'axios';

import { MetaDataService } from 'core/services/repository/metaData';
import RepositoryDataService from 'core/services/repository/repositoryData/RepositoryDataService';
import CompareCommitsService from 'core/services/repository/compareCommits/CompareCommitsService';

import { RepositoriesDataService } from '../core/services/repository/repositories';
import { DatasetsDataService } from './datasets';
import { DatasetVersionsDataService } from './datasetVersions';
import { DescActionDataService } from './descriptionActions';
import { ExperimentRunsDataService } from './experimentRuns';
import { ExperimentsDataService } from './experiments';
import { ProjectDataService } from './projects';
import { TagActionDataService } from './tagActions';

export default class ServiceFactory {
  public static addReponseErrorInterceptor(
    interceptor: (error: AxiosError) => void
  ) {
    axios.interceptors.response.use(
      config => config,
      async (error: AxiosError) => {
        await interceptor(error);
        return Promise.reject(error);
      }
    );
  }

  public static getDatasetsService() {
    return new DatasetsDataService();
  }

  public static getDatasetVersionsService() {
    return new DatasetVersionsDataService();
  }

  public static getProjectsService() {
    return new ProjectDataService();
  }

  public static getExperimentsService() {
    return new ExperimentsDataService();
  }

  public static getExperimentRunsService() {
    return new ExperimentRunsDataService();
  }

  public static crudTagsService(): TagActionDataService {
    return new TagActionDataService();
  }
  public static crudDescService() {
    return new DescActionDataService();
  }

  public static getRepositoriesService() {
    return new RepositoriesDataService();
  }

  public static getRepositoryDataService() {
    return new RepositoryDataService();
  }

  public static getMetaDataService() {
    return new MetaDataService();
  }

  public static getCompareCommitsService() {
    return new CompareCommitsService();
  }
}
