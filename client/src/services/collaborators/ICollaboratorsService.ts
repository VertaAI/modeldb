import { AxiosPromise } from 'axios';

import { UserAccess, ICollaboratorsWithOwner } from 'models/Project';
import User from 'models/User';

export interface ICollaboratorsService {
  sendInvitation(
    projectId: string,
    email: string,
    userAccess: UserAccess
  ): AxiosPromise<void>;
  sendInvitationWithInvitedUser(
    projectId: string,
    email: string,
    userAccess: UserAccess
  ): Promise<User>;
  changeOwner(projectId: string, newOwnerEmail: string): Promise<void>;
  changeAccessToProject(
    projectId: string,
    email: string,
    userAccess: UserAccess
  ): AxiosPromise<void>;
  removeAccessFromProject(projectId: string, email: string): AxiosPromise<void>;
  loadProjectCollaborators(projectId: string): Promise<User[]>;
  loadProjectCollaboratorsWithOwner(
    projectId: string,
    ownerId: string
  ): Promise<ICollaboratorsWithOwner>;
}
