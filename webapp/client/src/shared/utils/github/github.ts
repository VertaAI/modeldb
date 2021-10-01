import * as R from 'ramda';
import gitUrlParse from 'git-url-parse';

import { URL } from '../types';

export interface GitInfoFromUrl {
  baseUrl: string | undefined;
  repositoryFullName: string;
}

export interface IGithubRemoteRepoUrlComponents {
  baseUrl: string | undefined;
  repositoryFullName: string;
}

export type MaybeGithubRemoteRepoUrl =
  | { type: 'github'; value: IGithubRemoteRepoUrlComponents }
  | { type: 'unknown'; value: string };

export const makeGithubRemoteRepoUrl = (
  remoteRepoUrl: string
): MaybeGithubRemoteRepoUrl => {
  try {
    const parsed = gitUrlParse(remoteRepoUrl);
    return {
      type: 'github',
      value: {
        baseUrl: parsed.resource || undefined,
        repositoryFullName: parsed.full_name,
      },
    };
  } catch (e) {
    return { type: 'unknown', value: remoteRepoUrl };
  }
};

export const makeRepoUrl = (gitInfo: GitInfoFromUrl) => {
  if (gitInfo.baseUrl) {
    return `https://${gitInfo.baseUrl}/${gitInfo.repositoryFullName}`;
  }

  return gitInfo.repositoryFullName;
};

export const makeRepoBlobUrl = (
  components: IGithubRemoteRepoUrlComponents,
  { commitHash, execPath }: { commitHash: string; execPath: string }
): URL => {
  return `${makeRepoUrl(components)}/blob/${commitHash}/${execPath}`;
};

export const makeCompareCommitsUrl = ({
  repoWithCommitHash1,
  repoWithCommitHash2,
}: {
  repoWithCommitHash1: {
    url: GitInfoFromUrl;
    commitHash: string;
  };
  repoWithCommitHash2: {
    url: GitInfoFromUrl;
    commitHash: string;
  };
}) => {
  if (R.equals(repoWithCommitHash1.url, repoWithCommitHash2.url)) {
    const shortCommit1 = repoWithCommitHash1.commitHash.slice(0, 6);
    const shortCommit2 = repoWithCommitHash2.commitHash.slice(0, 6);
    return `${makeRepoUrl(
      repoWithCommitHash1.url
    )}/compare/${shortCommit1}..${shortCommit2}`;
  }
};

export const makeRepoShortName = (
  components: IGithubRemoteRepoUrlComponents
): string => {
  return components.repositoryFullName;
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
  return `${makeRepoUrl(components)}/commit/${commitHash}`;
};

export const makeTagUrl = (
  components: IGithubRemoteRepoUrlComponents,
  tag: string
): URL => {
  return `${makeRepoUrl(components)}/tree/${tag}`;
};

export const makeBranchUrl = (gitInfo: GitInfoFromUrl, branch: string) => {
  return `${makeRepoUrl(gitInfo)}/tree/${branch}`;
};
