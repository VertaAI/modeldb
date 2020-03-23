import { URL, SHHUrl } from '../types';

export type GithubRemoteRepoUrl = SHHUrl | URL;

export interface IGithubRemoteRepoUrlComponents {
  userName: string;
  repositoryInfo: { name: string; nameWithExtension: string };
}

export const parseGithubRemoteRepoUrl = (
  remoteRepoUrl: GithubRemoteRepoUrl
): IGithubRemoteRepoUrlComponents => {
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
  if (
    ['https://github.com', 'http://github.com', 'github.com'].some(t =>
      remoteRepoUrl.startsWith(t)
    )
  ) {
    const [_, pathname = ''] = remoteRepoUrl.split('github.com/');
    const userNameAndRepo = pathname.split('/');
    if (userNameAndRepo.length >= 2) {
      return {
        userName: userNameAndRepo[0],
        repositoryInfo: {
          name: userNameAndRepo[1],
          nameWithExtension: userNameAndRepo[1],
        },
      };
    } else {
      return {
        userName: '',
        repositoryInfo: { name: '', nameWithExtension: '' },
      };
    }
  }

  throw new Error('invalid repo url');
};

export const makeRepoUrl = (remoteRepoUrl: GithubRemoteRepoUrl): URL => {
  const components = parseGithubRemoteRepoUrl(remoteRepoUrl);
  return `https://github.com/${components.userName}/${
    components.repositoryInfo.name
  }`;
};

export const makeRepoShortName = (
  remoteRepoUrl: GithubRemoteRepoUrl
): string => {
  const components = parseGithubRemoteRepoUrl(remoteRepoUrl);
  return `${components.userName}/${
    components.repositoryInfo.nameWithExtension
  }`;
};

export const makeRepoUrlWithRepoShortName = (
  remoteRepoUrl: GithubRemoteRepoUrl
): { url: string; shortName: string } => {
  return {
    url: makeRepoUrl(remoteRepoUrl),
    shortName: makeRepoShortName(remoteRepoUrl),
  };
};

export const makeCommitUrl = (
  remoteRepoUrl: GithubRemoteRepoUrl,
  commitHash: string
): URL => {
  const components = parseGithubRemoteRepoUrl(remoteRepoUrl);
  return `https://github.com/${components.userName}/${
    components.repositoryInfo.name
  }/commit/${commitHash}`;
};

export const makeTagUrl = (
  remoteRepoUrl: GithubRemoteRepoUrl,
  tag: string
): URL => {
  return `${makeRepoUrl(remoteRepoUrl)}/tree/${tag}`;
};

export const makeBranchUrl = (
  remoteRepoUrl: GithubRemoteRepoUrl,
  branch: string
): URL => {
  return `${makeRepoUrl(remoteRepoUrl)}/tree/${branch}`;
};
