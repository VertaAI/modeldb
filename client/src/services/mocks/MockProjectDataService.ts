import axios from 'axios';
import MockAdapter from 'axios-mock-adapter';
import { IProjectDataService } from '../IProjectDataService';
import { ProjectDataService } from '../ProjectDataService';
import { projectsMock } from './projectsMock';

export class MockProjectDataService extends ProjectDataService implements IProjectDataService {
  public constructor() {
    super();

    const mock = new MockAdapter(axios);
    mock.onGet('/v1/project/getProjects').reply(200, { projects: projectsMock });
  }
}
