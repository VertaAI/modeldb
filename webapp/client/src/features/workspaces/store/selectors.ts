import { exhaustiveCheck } from 'shared/utils/exhaustiveCheck';
import * as Workspace from 'shared/models/Workspace';
import { IApplicationState } from 'store/store';
import { successfullCommunication } from 'shared/utils/redux/communication';

export const selectUserWorkspace = (
  state: IApplicationState
): Workspace.IUserWorkspace => {
  return {
    type: 'user',
    name: 'personal' as Workspace.IUserWorkspace['name'],
    id: 'id' as Workspace.IUserWorkspace['id'],
  };
};
export const selectWorkspaces = (
  state: IApplicationState
): Workspace.IUserWorkspaces => {
  const userWorkspace = selectUserWorkspace(state)!;
  return {
    user: userWorkspace,
  };
};

export const selectCurrentWorkspaceName = (state: IApplicationState) => {
  return selectCurrentWorkspace(state).name as Workspace.WorkspaceName;
};

export const selectCurrentWorkspace = (state: IApplicationState) => {
  const shortWorkspace = state.workspaces.data.currentWorkspace;
  const workspaces = selectWorkspaces(state);

  if (!shortWorkspace) {
    return workspaces.user;
  }

  switch (shortWorkspace.type) {
    case 'user': {
      return workspaces.user;
    }
    case 'unknown': {
      return shortWorkspace;
    }
    default:
      return exhaustiveCheck(shortWorkspace, '');
  }
};

export const selectWorkspaceByName = (
  state: IApplicationState,
  name: Workspace.IWorkspace['name']
) => {
  const workspaces = selectWorkspaces(state);
  return Workspace.findWorkspaceByName(name, workspaces);
};

export const selectLoadingUserWorkspaces = (state: IApplicationState) =>
  successfullCommunication;
