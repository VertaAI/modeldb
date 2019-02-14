import { UserAccess } from 'models/Project';

export interface ICollaboratorsService {
  sendInvitation(projectId: string, email: string, userAccess: UserAccess): Promise<void>;
  changeOwner(projectId: string, newOwnerEmail: string): Promise<void>;
}
