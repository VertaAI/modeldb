import { UserAccess } from '../models/Project';
import { ICollaboratorsService } from './ICollaboratorsService';

export default class CollaboratorsService implements ICollaboratorsService {
  public sendInvitation(projectId: string, email: string, userAccess: UserAccess): Promise<void> {
    return Promise.resolve();
  }

  public changeOwner(projectId: string, newOwnerEmail: string): Promise<void> {
    return Promise.resolve();
  }

  public changeAccessToProject(projectId: string, email: string, userAccess: UserAccess): Promise<void> {
    return Promise.resolve();
  }

  public removeAccessFromProject(projectId: string, email: string): Promise<void> {
    return Promise.resolve();
  }
}
