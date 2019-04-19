import axios, { AxiosPromise, AxiosRequestConfig } from 'axios';
import { UserAccess, ICollaboratorsWithOwner } from 'models/Project';
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
  ): AxiosPromise<void> {
    return axios.post(
      '/v1/modeldb/collaborator/addOrUpdateProjectCollaborator',
      {
        params: {
          entity_id: projectId,
          share_with: email,
          collaborator_type: convertUserAccessToServer(userAccess),
          date_created: new Date().getMilliseconds(),
          message: 'Please refer shared project for your invantion',
        },
      }
    );
  }

  public changeOwner(projectId: string, newOwnerEmail: string): Promise<void> {
    return Promise.resolve();
  }

  public changeAccessToProject(
    projectId: string,
    userId: string,
    userAccess: UserAccess
  ): AxiosPromise<void> {
    return axios.post('/v1/modeldb/collaborator/removeProjectCollaborator', {
      params: {
        entity_id: projectId,
        share_with: userId,
        collaborator_type: convertUserAccessToServer(userAccess),
        date_updated: new Date().getMilliseconds(),
        message: 'user comment',
      },
    });
  }

  public removeAccessFromProject(
    projectId: string,
    userId: string
  ): AxiosPromise<void> {
    return axios.post(
      '/v1/modeldb/collaborator/addOrUpdateProjectCollaborator',
      {
        params: {
          entity_id: projectId,
          share_with: userId,
          date_deleted: new Date().getMilliseconds(),
        },
      }
    );
  }

  public loadProjectCollaboratorsWithOwner(
    projectId: string,
    ownerId: string
  ): Promise<ICollaboratorsWithOwner> {
    const ownerPromise = this.loadProjectOwner(ownerId);
    const collaboratorsPromise = this.loadProjectCollaborators(projectId);
    return Promise.all([ownerPromise, collaboratorsPromise]).then(res => ({
      owner: res[0],
      collaborators: res[1],
    }));
  }

  public loadProjectOwner(userId: string): Promise<User> {
    return axios
      .get<IServerUserInfo>('/uac-proxy/v1/uac/getUser', {
        params: { user_id: userId },
        paramsSerializer: (params: any) => `user_id=${params.user_id}`,
      })
      .then(res => {
        const owner = convertServerUserToClient(0, res.data);
        owner.access = UserAccess.Owner;
        return owner;
      });
  }

  public loadProjectCollaborators(projectId: string): Promise<User[]> {
    const config: AxiosRequestConfig = {
      params: {
        entity_id: projectId,
      },
    };
    return axios
      .get<ILoadProjectCollaboratorsResponse>(
        '/v1/modeldb/collaborator/getProjectCollaborators',
        config
      )
      .then(res => {
        const collaboratorsPromisses = res.data.shared_users.map(userEntity => {
          return axios.get<IServerUserInfo>('/uac-proxy/v1/uac/getUser', {
            params: { user_id: userEntity.user_id },
            paramsSerializer: (params: any) => `user_id=${params.user_id}`,
          });
        });
        return Promise.all(collaboratorsPromisses).then(collabs =>
          collabs.map((r, i) => {
            const userAccess = convertServerUserAccessToClient(
              res.data.shared_users[i].collaborator_type
            );
            return convertServerUserToClient(userAccess, r.data);
          })
        );
      }) as any;
  }
}

const convertServerUserToClient = (
  access: UserAccess,
  serverUser: IServerUserInfo
): User => {
  const user = new User(serverUser.user_id, serverUser.email);
  user.name = serverUser.full_name;
  user.access = access;
  return user;
};

const convertServerUserAccessToClient = (serverUserAccess: any): UserAccess => {
  switch (serverUserAccess) {
    case 0:
      return UserAccess.Read;
    case 1:
      return UserAccess.Write;
    default:
      return UserAccess.Read;
  }
};

const convertUserAccessToServer = (userAccess: UserAccess) => {
  switch (userAccess) {
    case UserAccess.Read:
      return 0;
    case UserAccess.Write:
      return 1;
  }
};

interface IServerUserInfo {
  user_id: string;
  full_name: string;
  email: string;
  roles: string;
  id_service_provider: string;
}

interface ILoadProjectCollaboratorsResponse {
  shared_users: Array<{
    user_id: string;
    collaborator_type: number;
  }>;
}
