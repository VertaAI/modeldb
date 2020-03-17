import {
  IGitCodeBlob,
  ICodeBlob,
  INotebookCodeBlob,
  makeGitCodeBlobRemoteRepoUrl,
} from 'core/shared/models/Repository/Blob/CodeBlob';

import { convertPathDatasetBlob } from './DatasetBlob';

export const convertServerCodeBlobToClient = (
  serverCode: any
): ICodeBlob | undefined => {
  if (serverCode.git) {
    return {
      category: 'code',
      data: convertServerGitBlob(serverCode.git),
    };
  }
  if (serverCode.notebook) {
    return {
      category: 'code',
      data: convertServerNotebookBlob(serverCode.notebook),
    };
  }

  throw new Error(`${serverCode} unknown code type`);
};

export const convertServerNotebookBlob = (
  serverNotebook: any
): INotebookCodeBlob => {
  return {
    type: 'notebook',
    data: {
      gitBlob: serverNotebook.git_repo
        ? convertServerGitBlob(serverNotebook.git_repo)
        : null,
      path: serverNotebook.path
        ? convertPathDatasetBlob(serverNotebook.path)
        : null,
    },
  };
};

export const convertServerGitBlob = (serverGit: any): IGitCodeBlob => {
  return {
    type: 'git',
    data: {
      branch: serverGit.branch,
      commitHash: serverGit.hash,
      isDirty: serverGit.is_dirty,
      remoteRepoUrl: makeGitCodeBlobRemoteRepoUrl(serverGit.repo),
      tag: serverGit.tag,
    },
  };
};
