import { IArtifact } from './Artifact';

export interface IArtifactCodeVersion {
  type: 'artifact';
  data: IArtifact;
}

type IIsDirtyType = 'UNKNOWN' | 'TRUE' | 'FALSE';

export interface IGitCodeVersionData {
  execPath?: string;
  remoteRepoUrl?: string;
  commitHash?: string;
  isDirty?: IIsDirtyType;
}
export interface IGitCodeVersion {
  type: 'git';
  data: IGitCodeVersionData;
}
export type ICodeVersion = IArtifactCodeVersion | IGitCodeVersion;

export interface IGitRemoteRepoUrlInfo {
  userName: string;
  repositoryInfo: { name: string; nameWithExtension: string };
}

export const parseGitRemoteRepoUrl = (
  remoteRepoUrl: string
): {
  userName: string;
  repositoryInfo: { name: string; nameWithExtension: string };
} => {
  if (remoteRepoUrl.startsWith('git@')) {
    const [_, userName, repoName] = /git@github.com:(.+)\/(.+).git/.exec(
      remoteRepoUrl
    );
    return {
      userName,
      repositoryInfo: {
        name: repoName,
        nameWithExtension: `${repoName}.git`,
      },
    };
  }
  console.assert('!!!!!');
  return {} as any;
};
