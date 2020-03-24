import User from 'models/User';
import * as Workspace from 'models/Workspace';

export interface IRepository extends Workspace.IEntityWithShortWorkspace {
  id: string;
  name: string;
  dateCreated: Date;
  dateUpdated: Date;
  labels: Label[];
  owner: User;
}

export type Label = string;

export interface IRepositoryNamedIdentification {
  name: IRepository['name'];
  workspaceName: Workspace.IWorkspace['name'];
}
