import axios from 'axios';
import MockAdapter from 'axios-mock-adapter';

import { IProjectDataService } from 'services/IProjectDataService';
import { ProjectDataService } from 'services/ProjectDataService';

import { projectsMock } from './projectsMock';

export class MockProjectDataService extends ProjectDataService implements IProjectDataService {
  public constructor() {
    super();

    const mock = new MockAdapter(axios);
    mock.onGet('/getProjects').reply(200, { projects: projectsMock });
  }
}
