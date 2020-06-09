import { exhaustiveCheck } from 'core/shared/utils/exhaustiveCheck';
import { RecordFromUnion } from 'core/shared/utils/types';
import User from './User';

export type WorkspaceType = 'user' | 'unknown';

export type WorkspaceId = User['id'];
export type UserWorkspaceName = User['username'];
export type WorkspaceName = UserWorkspaceName;

export type IWorkspace = RecordFromUnion<
  WorkspaceType,
  {
    user: {
      type: 'user';
      id: string;
      name: string;
    };
    unknown: {
      type: 'unknown';
      name: UserWorkspaceName;
    };
  }
>[WorkspaceType];

export type IUserWorkspace = Extract<IWorkspace, { type: 'user' }>;
export interface IUserWorkspaces {
  user: IUserWorkspace;
}

export type IShortWorkspace = IUserShortWorkspace;
export type IUserShortWorkspace = Omit<IUserWorkspace, 'name'>;
export type IUnknownWorkspace = Extract<IWorkspace, { type: 'unknown' }>;
export interface IEntityWithShortWorkspace {
  shortWorkspace: IShortWorkspace;
}

export type ICurrentWorkspace = { type: 'user' } | IUnknownWorkspace;
export const workspaceToCurrentShortWorkspace = (
  workspace: IWorkspace
): ICurrentWorkspace => {
  switch (workspace.type) {
    case 'unknown':
      return { type: 'unknown', name: workspace.name };
    case 'user':
      return { type: 'user' };
    default:
      return exhaustiveCheck(workspace, '');
  }
};

export const getDisplayedWorkspaceName = (workspace: IWorkspace) => {
  switch (workspace.type) {
    case 'user':
      return 'Personal';
    case 'unknown':
      return { type: 'unknown', name: workspace.name };
    default:
      return exhaustiveCheck(workspace, '');
  }
};

export const getOrderedWorkspacesByType = (
  workspaces: IUserWorkspaces
): [IUserWorkspace] => {
  return [workspaces.user];
};

export const findWorkspaceByName = (
  workspaceName: IWorkspace['name'],
  userWorkspaces: IUserWorkspaces
) => {
  return [userWorkspaces.user].find(
    workspace => workspace.name === workspaceName
  );
};
