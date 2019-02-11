import { UserAccess } from '../models/Project';
import { ICollaboratorsService } from './ICollaboratorsService';

export default class CollaboratorsService implements ICollaboratorsService {
  public sendInvitation(email: string, userAccess: UserAccess): Promise<void> {
    return Promise.resolve();
  }
}
