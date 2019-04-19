import axios, { AxiosPromise, AxiosRequestConfig } from 'axios';
import { UserAccess } from 'models/Project';
import User from 'models/User';

import { BaseDataService } from '../BaseDataService';
import { ICollaboratorsService } from './ICollaboratorsService';

export default class CollaboratorsService extends BaseDataService
  implements ICollaboratorsService {
  constructor() {
    super();
  }

  public sendInvitation(
    projectId: string,
    email: string,
    userAccess: UserAccess
  ): Promise<void> {
    return Promise.resolve();
  }

  public changeOwner(projectId: string, newOwnerEmail: string): Promise<void> {
    return Promise.resolve();
  }

  public changeAccessToProject(
    projectId: string,
    email: string,
    userAccess: UserAccess
  ): Promise<void> {
    return Promise.resolve();
  }

  public removeAccessFromProject(
    projectId: string,
    email: string
  ): Promise<void> {
    return Promise.resolve();
  }

  public loadProjectCollaborators(projectId: string): AxiosPromise<User[]> {
    const config: AxiosRequestConfig = {
      params: {
        entity_id: projectId,
      },
    };
    axios
      .get<ILoadProjectCollaboratorsResponse[]>(
        '/v1/modeldb/collaborator/getProjectCollaborators',
        config
      )
      .then(res => {
        const data = res.data;
        data.map(x => {
          return axios.get();
        });
      });
    return [] as any;
  }
}

interface ILoadProjectCollaboratorsResponse {
  user_id: string;
  collaborator_type: number;
}
