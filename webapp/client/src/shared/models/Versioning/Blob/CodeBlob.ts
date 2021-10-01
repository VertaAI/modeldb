import { MaybeGithubRemoteRepoUrl } from 'shared/utils/github/github';

import {
  IPathDatasetComponentBlob,
  IPathDatasetComponentBlobDiff,
} from './DatasetBlob';
import { IElementDiff, IBlobDiff } from './Diff';

export interface ICodeBlob {
  category: 'code';
  data: IGitCodeBlob | INotebookCodeBlob;
}

export interface IGitCodeBlob {
  type: 'git';
  data: {
    remoteRepoUrl: MaybeGithubRemoteRepoUrl;
    commitHash: string | null;
    branch: string | null;
    tag: string | null;
    isDirty: boolean | null;
  };
}

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
