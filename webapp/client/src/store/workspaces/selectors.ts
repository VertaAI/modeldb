import { exhaustiveCheck } from 'core/shared/utils/exhaustiveCheck';
import * as Workspace from 'models/Workspace';
import { IApplicationState } from 'store/store';
import { successfullCommunication } from 'core/shared/utils/redux/communication';

export const selectUserWorkspace = (
  state: IApplicationState
): Workspace.IUserWorkspace => {
  return {
    type: 'user',
    name: 'kirill_gulyaev' as any,
    id: '1068' as any,
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
  return selectCurrentWorkspace(state).name;
};

export const selectCurrentWorkspace = (state: IApplicationState) => {
  const shortWorkspace = state.workspaces.data.currentWorkspace;
  const workspaces = selectWorkspaces(state);

  console.log(workspaces);

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
