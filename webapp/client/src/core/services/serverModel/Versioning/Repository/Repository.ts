import { IServerEntityWithWorkspace } from 'services/serverModel/Workspace/Workspace';

export interface IServerRepository extends IServerEntityWithWorkspace {
  id: string;
  name: string;
  date_created: string;
  date_updated: string;
  owner: string;
}
