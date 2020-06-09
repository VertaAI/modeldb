import { IShortWorkspace } from 'core/shared/models/Workspace';

export interface IServerEntityWithWorkspace {
  workspace_id: IShortWorkspace['id'];
  workspace_type: 'USER';
}
