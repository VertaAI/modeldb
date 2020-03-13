import { exhaustiveCheck } from 'core/shared/utils/exhaustiveCheck';
import {
  IWorkspace,
  IShortWorkspace,
  IEntityWithShortWorkspace,
} from 'models/Workspace';
import { IServerEntityWithWorkspace } from 'services/serverModel/Workspace/Workspace';

export const addWorkspaceName = <T>(workspaceName: IWorkspace['name']) => (
  request: T
): T & IServerEntityWithWorkspaceName => {
  return { ...request, workspace_name: workspaceName };
};

export interface IServerEntityWithWorkspaceName {
  workspace_name: IWorkspace['name'];
}

export const convertServerShortWorkspaceToClient = (
  entity: IServerEntityWithWorkspace
): IShortWorkspace => {
  switch (entity.workspace_type) {
    case 'USER': {
      return {
        id: entity.workspace_id as any,
        type: 'user',
      };
    }
    default:
      return exhaustiveCheck(entity.workspace_type, '');
  }
};

export const addShortWorkspace = <T extends IEntityWithShortWorkspace>(
  serverEntity: IServerEntityWithWorkspace,
  entity: IEntityWithShortWorkspace
): IEntityWithShortWorkspace => {
  const shortWorkspace = convertServerShortWorkspaceToClient(serverEntity);
  return { ...entity, shortWorkspace };
};
