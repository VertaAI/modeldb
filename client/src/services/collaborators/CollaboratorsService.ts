import axios, { AxiosPromise } from 'axios';
import { UserAccess } from 'models/Project';
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
  ): AxiosPromise<boolean> {
    const axiosConfig = {
      params: {
        entity_id: projectId,
        share_with: email,
        collaborator_type: 0,
      },
    };
    return axios.get<boolean>(
      '/v1/modeldb/collaborator/addOrUpdateProjectCollaborator',
      axiosConfig
    );
  }
  public removeAccessFromProject(
    projectId: string,
    email: string
  ): Promise<void> {
    return Promise.resolve();
  }
}
