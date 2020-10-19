import * as R from 'ramda';

import { URL, SHHUrl } from '../types';

export type GithubRemoteRepoUrl = SHHUrl | URL;

export interface IGithubRemoteRepoUrlComponents {
  userName: string;
  repositoryInfo: { name: string; nameWithExtension: string };
}

export type MaybeGithubRemoteRepoUrl =
  | { type: 'github'; value: IGithubRemoteRepoUrlComponents }
  | { type: 'unknown'; value: string };

export type Result<T, Error> =
  | { type: 'success'; data: T }
  | { type: 'error'; error: Error };

export const makeGithubRemoteRepoUrl = (
  remoteRepoUrl: string
): MaybeGithubRemoteRepoUrl => {
  const res = parseGithubRemoteRepoUrl(remoteRepoUrl);
  if (res.type === 'success') {
    return { type: 'github', value: res.data };
  } else {
    return { type: 'unknown', value: remoteRepoUrl };
  }
};

export const parseGithubRemoteRepoUrl = (
  remoteRepoUrl: GithubRemoteRepoUrl
): Result<IGithubRemoteRepoUrlComponents, string> => {
  if (remoteRepoUrl.startsWith('git@')) {
    const [, userName, repoName] = /git@github.com:(.+)\/(.+).git/.exec(
      remoteRepoUrl
    );
    return {
      type: 'success',
      data: {
        userName,
        repositoryInfo: {
          name: repoName,
          nameWithExtension: `${repoName}.git`,
        },
      },
    };
  }
  if (
    ['https://github.com', 'http://github.com', 'github.com'].some((t) =>
      remoteRepoUrl.startsWith(t)
    )
  ) {
    const [, pathname = ''] = remoteRepoUrl.split('github.com/');
    const userNameAndRepo = pathname.split('/');
    if (userNameAndRepo.length >= 2) {
      return {
        type: 'success',
        data: {
          userName: userNameAndRepo[0],
          repositoryInfo: {
            name: userNameAndRepo[1],
            nameWithExtension: userNameAndRepo[1],
          },
        },
      };
    } else {
      return {
        type: 'success',
        data: {
          userName: '',
          repositoryInfo: { name: '', nameWithExtension: '' },
        },
      };
    }
  }

  return { type: 'error', error: 'invalid repo url' };
};

export const makeRepoUrl = (
  components: IGithubRemoteRepoUrlComponents
): URL => {
  return `https://github.com/${components.userName}/${components.repositoryInfo.name}`;
};

export const makeRepoBlobUrl = (
  components: IGithubRemoteRepoUrlComponents,
  { commitHash, execPath }: { commitHash: string; execPath: string }
): URL => {
  return `https://github.com/${components.userName}/${components.repositoryInfo.name}/blob/${commitHash}/${execPath}`;
};

export const makeCompareCommitsUrl = ({
  repoWithCommitHash1,
  repoWithCommitHash2,
}: {
  repoWithCommitHash1: {
    url: IGithubRemoteRepoUrlComponents;
    commitHash: string;
  };
  repoWithCommitHash2: {
    url: IGithubRemoteRepoUrlComponents;
    commitHash: string;
  };
}) => {
  if (R.equals(repoWithCommitHash1.url, repoWithCommitHash2.url)) {
    const {
      userName,
      repositoryInfo: { name: repoName },
    } = repoWithCommitHash1.url;
    const shortCommit1 = repoWithCommitHash1.commitHash.slice(0, 6);
    const shortCommit2 = repoWithCommitHash2.commitHash.slice(0, 6);
    return `https://github.com/${userName}/${repoName}/compare/${shortCommit1}..${shortCommit2}`;
  }
};

export const makeRepoShortName = (
  components: IGithubRemoteRepoUrlComponents
): string => {
  return `${components.userName}/${components.repositoryInfo.nameWithExtension}`;
};

export const makeRepoUrlWithRepoShortName = (
  components: IGithubRemoteRepoUrlComponents
): { url: string; shortName: string } => {
  return {
    url: makeRepoUrl(components),
    shortName: makeRepoShortName(components),
  };
};

export const makeCommitUrl = (
  components: IGithubRemoteRepoUrlComponents,
  commitHash: string
): URL => {
  return `https://github.com/${components.userName}/${components.repositoryInfo.name}/commit/${commitHash}`;
};

export const makeTagUrl = (
  components: IGithubRemoteRepoUrlComponents,
  tag: string
): URL => {
  return `${makeRepoUrl(components)}/tree/${tag}`;
};

export const makeBranchUrl = (
  components: IGithubRemoteRepoUrlComponents,
  branch: string
): URL => {
  return `${makeRepoUrl(components)}/tree/${branch}`;
};
