import { ICurrentWorkspace } from 'shared/models/Workspace';

export interface IWorkspaces {
  data: {
    currentWorkspace: ICurrentWorkspace;
  };
}
