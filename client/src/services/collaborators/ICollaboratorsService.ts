import { AxiosPromise } from 'axios';

import { UserAccess } from 'models/Project';
import User from 'models/User';

export interface ICollaboratorsService {
  sendInvitation(
    projectId: string,
    email: string,
    userAccess: UserAccess
  ): Promise<void>;
  changeOwner(projectId: string, newOwnerEmail: string): Promise<void>;
  changeAccessToProject(
    projectId: string,
    email: string,
    userAccess: UserAccess
  ): Promise<void>;
  removeAccessFromProject(projectId: string, email: string): Promise<void>;
  loadProjectCollaborators(projectId: string): AxiosPromise<User[]>;
}
