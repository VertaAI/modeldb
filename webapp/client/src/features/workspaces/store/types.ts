import { ICurrentWorkspace } from 'core/shared/models/Workspace';

export interface IWorkspaces {
  data: {
    currentWorkspace: ICurrentWorkspace;
  };
}
