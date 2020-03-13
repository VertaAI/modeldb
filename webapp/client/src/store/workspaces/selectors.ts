import { successfullCommunication } from 'core/shared/utils/redux/communication';
import * as Workspace from 'models/Workspace';
import routes from 'routes';
import { IApplicationState } from 'store/store';

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
  const userWorkspace = selectUserWorkspace(state);
  return {
    user: userWorkspace,
  };
};

export const selectCurrentWorkspaceNameOrDefault = (
  state: IApplicationState
) => {
  const params = routes.workspace.getMatch(window.location.pathname, false);
  const defaultWorkspaceName = selectDefaultWorkspaceName(state);
  return params && selectWorkspaceByName(state, params.workspaceName)
    ? params.workspaceName
    : defaultWorkspaceName;
};

export const selectCurrentWorkspaceOrDefault = (state: IApplicationState) => {
  const params = routes.workspace.getMatch(window.location.pathname, false);
  const defaultWorkspace = selectDefaultWorkspace(state);
  const currentWorkspace =
    params && selectWorkspaceByName(state, params.workspaceName);
  return currentWorkspace ? currentWorkspace : defaultWorkspace;
};

export const selectDefaultWorkspaceName = (state: IApplicationState) => {
  return selectUserWorkspace(state).name;
};

export const selectDefaultWorkspace = (state: IApplicationState) => {
  return selectUserWorkspace(state);
};

export const selectWorkspaceByName = (
  state: IApplicationState,
  name: Workspace.IWorkspace['name']
) => {
  const workspaces = selectWorkspaces(state);
  return [workspaces.user].find(workspace => workspace.name === name);
};

export const selectLoadingUserWorkspaces = (state: IApplicationState) =>
  successfullCommunication;
