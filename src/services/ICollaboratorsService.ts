import { UserAccess } from 'models/Project';

export interface ICollaboratorsService {
  sendInvitation(email: string, userAccess: UserAccess): Promise<void>;
}
