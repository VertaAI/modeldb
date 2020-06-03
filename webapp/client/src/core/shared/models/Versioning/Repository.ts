import User, { CurrentUser } from 'models/User';
import * as Workspace from 'models/Workspace';

import { RepositoryBranches, CommitTag } from './RepositoryData';

export interface IRepository
  extends Workspace.IEntityWithShortWorkspace {
  id: string;
  name: string;
  dateCreated: Date;
  dateUpdated: Date;
  labels: Label[];
  owner: User;
}

export interface IBranchesAndTags {
  branches: RepositoryBranches;
  tags: CommitTag[];
}

export type IRepositoryWithTagsAndBranches = IRepository & IBranchesAndTags;

export type RepositoryVisibility = 'private' | 'public' | 'organizationPublic';

export type Label = string;

export interface IRepositoryNamedIdentification {
  name: IRepository['name'];
  workspaceName: Workspace.IWorkspace['name'];
}

export const isRepositoryDeletingAvailable = (
  currentUserId: CurrentUser['id'],
  repository: IRepository
) => {
  return repository.owner.id === currentUserId;
};
