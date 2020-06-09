import { IWorkspace } from 'shared/models/Workspace';

import { IArtifact } from './Artifact';

export interface IRelease {
  packageId: string;
  releaseName: string;
  releaseTag: string;
  experimentRunId: string;
  assets: IArtifact[];
}

export interface IReleaseSettings {
  workspaceName: IWorkspace['name'];
  releaseName: string;
  releaseTag: string;
  experimentRunId?: string;
  assets?: IArtifact[];
}

export type ReleaseExperimentRunInfo = Pick<
  IRelease,
  'assets' | 'experimentRunId'
>;
