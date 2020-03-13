import axios, { AxiosError } from 'axios';
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
}
