import {
  GithubRemoteRepoUrl,
  parseGithubRemoteRepoUrl,
} from 'core/shared/utils/github/github';

import { IPathDatasetComponentBlob, IPathDatasetComponentBlobDiff } from './DatasetBlob';
import { IElementDiff, IBlobDiff } from './Diff';

export interface ICodeBlob {
  category: 'code';
  data: IGitCodeBlob | INotebookCodeBlob;
}

export interface IGitCodeBlob {
  type: 'git';
  data: {
    remoteRepoUrl: GitCodeBlobRemoteRepoUrl;
    commitHash: string | null;
    branch: string | null;
    tag: string | null;
    isDirty: boolean | null;
  };
}

export const makeGitCodeBlobRemoteRepoUrl = (
  remoteRepoUrl: string
): GitCodeBlobRemoteRepoUrl => {
  try {
    parseGithubRemoteRepoUrl(remoteRepoUrl);
    return { type: 'github', value: remoteRepoUrl };
  } catch (e) {
    return { type: 'unknown', value: remoteRepoUrl };
  }
};
export type GitCodeBlobRemoteRepoUrl =
  | { type: 'github'; value: GithubRemoteRepoUrl }
  | { type: 'unknown'; value: string };

export interface INotebookCodeBlob {
  type: 'notebook';
  data: {
    path: IPathDatasetComponentBlob | null;
    gitBlob: IGitCodeBlob | null;
  };
}

// diff

export type ICodeBlobDiff = IGitCodeDiff | INotebookCodeDiff;

export type IGitCodeDiff = IBlobDiff<
  IGitCodeDiffData,
  ICodeBlob['category'],
  IGitCodeBlob['type']
>;
export type IGitCodeDiffData = IElementDiff<IGitCodeBlob>;

export type INotebookCodeDiff = IBlobDiff<
  {
    git?: IGitCodeDiffData;
    path?: IPathDatasetComponentBlobDiff;
  },
  ICodeBlob['category'],
  INotebookCodeBlob['type']
>;
