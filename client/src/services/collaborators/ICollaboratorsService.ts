import { UserAccess } from 'models/Project';
import { AxiosPromise } from 'axios';

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
  ): AxiosPromise<boolean>;
  removeAccessFromProject(projectId: string, email: string): Promise<void>;
}
