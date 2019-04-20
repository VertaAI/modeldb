import axios from 'axios';
import MockAdapter from 'axios-mock-adapter';

import { expRunsMocks } from '../mocks/expRunsMock';
import ExperimentRunsDataService from './ExperimentRunsDataService';
import { IExperimentRunsDataService } from './IExperimentRunsDataService';

export default class MockExperimentRunsDataService
  extends ExperimentRunsDataService
  implements IExperimentRunsDataService {
  public constructor() {
    super();

    const mock = new MockAdapter(axios);
    mock
      .onGet('/v1/modeldb/experiment-run/getExperimentRunsInProject')
      .reply(config => {
        return [
          200,
          {
            experiment_runs: expRunsMocks.filter(
              x => x.project_id === config.params.project_id
            ),
          },
        ];
      });
  }
}
