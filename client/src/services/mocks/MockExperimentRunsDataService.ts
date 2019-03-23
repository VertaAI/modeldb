import axios from 'axios';
import MockAdapter from 'axios-mock-adapter';
import ExperimentRunsDataService from 'services/ExperimentRunsDataService';
import { IExperimentRunsDataService } from 'services/IExperimentRunsDataService';
import { expRunsMocks } from './expRunsMock';

export class MockExperimentRunsDataService extends ExperimentRunsDataService implements IExperimentRunsDataService {
  public constructor() {
    super();

    const mock = new MockAdapter(axios);
    mock.onGet('/getExperimentRunsInProject').reply(config => {
      return [200, { experiment_runs: expRunsMocks.filter(x => x.project_id === config.params.project_id) }];
    });
  }
}
