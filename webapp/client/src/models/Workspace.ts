import { exhaustiveCheck } from 'core/shared/utils/exhaustiveCheck';
import { RecordFromUnion } from 'core/shared/utils/types';

export type WorkspaceType = 'user';

export type IWorkspace = RecordFromUnion<
  WorkspaceType,
  {
    user: {
      type: 'user';
      id: string;
      name: string;
    };
  }
>[WorkspaceType];

export type IUserWorkspace = Extract<IWorkspace, { type: 'user' }>;
export interface IUserWorkspaces {
  user: IUserWorkspace;
}

export type IShortWorkspace = IUserShortWorkspace;
export type IUserShortWorkspace = Omit<IUserWorkspace, 'name'>;
export interface IEntityWithShortWorkspace {
  shortWorkspace: IShortWorkspace;
}

export const getDisplayedWorkspaceName = (workspace: IWorkspace) => {
  switch (workspace.type) {
    case 'user':
      return 'Personal';
    default:
      return exhaustiveCheck(workspace.type, '');
  }
};

export const getOrderedWorkspacesByType = (
  workspaces: IUserWorkspaces
): [IUserWorkspace] => {
  return [workspaces.user];
};
