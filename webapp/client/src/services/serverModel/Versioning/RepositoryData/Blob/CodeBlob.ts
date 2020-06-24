import {
  IGitCodeBlob,
  ICodeBlob,
  INotebookCodeBlob,
} from 'shared/models/Versioning/Blob/CodeBlob';
import { makeGithubRemoteRepoUrl } from 'shared/utils/github/github';

import { convertServerDatasetPathComponent } from './DatasetBlob';

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
        ? convertServerDatasetPathComponent(serverNotebook.path)
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
      remoteRepoUrl: makeGithubRemoteRepoUrl(serverGit.repo),
      tag: serverGit.tag,
    },
  };
};
