import { IShortWorkspace } from 'models/Workspace';

export interface IServerEntityWithWorkspace {
  workspace_id: IShortWorkspace['id'];
  workspace_type: 'USER';
}
