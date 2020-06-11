import { MaybeGithubRemoteRepoUrl } from '../utils/github/github';
import { IArtifact } from './Artifact';

export interface IArtifactCodeVersion {
  type: 'artifact';
  data: IArtifact;
}

type IIsDirtyType = 'UNKNOWN' | 'TRUE' | 'FALSE';

export interface IGitCodeVersionData {
  execPath?: string;
  remoteRepoUrl?: MaybeGithubRemoteRepoUrl;
  commitHash?: string;
  isDirty?: IIsDirtyType;
}
export interface IGitCodeVersion {
  type: 'git';
  data: IGitCodeVersionData;
}
export type ICodeVersion = IArtifactCodeVersion | IGitCodeVersion;
