import { ApolloClient } from 'apollo-boost';
import axios, { AxiosError } from 'axios';

import HighLevelSearchService from 'services/highLevelSearch/HighLevelSearchService';

import { CommentsService } from './comments';
import { DatasetsDataService } from './datasets';
import { DatasetVersionsDataService } from './datasetVersions';
import { DescActionDataService } from './descriptionActions';
import { ExperimentRunsDataService } from './experimentRuns';
import { ExperimentsDataService } from './experiments';
import { ProjectDataService } from './projects';
import { TagActionDataService } from './tagActions';
import { RepositoriesDataService } from './versioning/repositories';

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

  public static getCommentsService() {
    return new CommentsService();
  }

  public static getHighLevelSearchService(apolloClient: ApolloClient<any>) {
    return new HighLevelSearchService(apolloClient);
  }
}
