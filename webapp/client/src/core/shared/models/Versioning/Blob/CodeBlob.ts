import {
  GithubRemoteRepoUrl,
  parseGithubRemoteRepoUrl,
} from 'core/shared/utils/github/github';
import matchBy from 'core/shared/utils/matchBy';

import { IPathDatasetBlob } from './DatasetBlob';
import { GenericDiff } from './Diff';

export interface ICodeBlob {
  category: 'code';
  data: IGitCodeBlob | INotebookCodeBlob;
}
export type ICodeBlobDiff = IGitCodeDiff | INotebookCodeDiff;

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

export type IGitCodeDiff = GenericDiff<
  Omit<ICodeBlob, 'data'> & { data: IGitCodeBlob },
  ICodeBlob['category'],
  IGitCodeBlob['type']
>;

export interface INotebookCodeBlob {
  type: 'notebook';
  data: {
    path: IPathDatasetBlob | null;
    gitBlob: IGitCodeBlob | null;
  };
}
export type INotebookCodeDiff = GenericDiff<
  Omit<ICodeBlob, 'data'> & { data: INotebookCodeBlob },
  ICodeBlob['category'],
  INotebookCodeBlob['type']
>;

export type IPathDatasetBlobDiff = GenericDiff<
  IPathDatasetBlob,
  ICodeBlob['category'],
  'path'
>;
export const getPathDatasetBlobDiffFromNotebook = (
  diff: INotebookCodeDiff
): IPathDatasetBlobDiff | null => {
  return matchBy(diff, 'diffType')({
    added: addedDiff => {
      if (!addedDiff.blob.data.data.path) {
        return null;
      }
      const res: IPathDatasetBlobDiff = {
        category: 'code',
        type: 'path',
        diffType: 'added',
        blob: addedDiff.blob.data.data.path,
        location: addedDiff.location,
      };
      return res;
    },
    deleted: deletedDiff => {
      if (!deletedDiff.blob.data.data.path) {
        return null;
      }
      const res: IPathDatasetBlobDiff = {
        category: 'code',
        type: 'path',
        diffType: 'deleted',
        blob: deletedDiff.blob.data.data.path,
        location: deletedDiff.location,
      };
      return res;
    },
    updated: updatedDiff => {
      if (
        !updatedDiff.blobA.data.data.path ||
        !updatedDiff.blobB.data.data.path
      ) {
        return null;
      }
      if (
        updatedDiff.blobA.data.data.path &&
        !updatedDiff.blobB.data.data.path
      ) {
        const res: IPathDatasetBlobDiff = {
          category: 'code',
          type: 'path',
          diffType: 'deleted',
          blob: updatedDiff.blobA.data.data.path,
          location: updatedDiff.location,
        };
        return res;
      }
      if (
        !updatedDiff.blobA.data.data.path &&
        updatedDiff.blobB.data.data.path
      ) {
        const res: IPathDatasetBlobDiff = {
          category: 'code',
          type: 'path',
          diffType: 'added',
          blob: updatedDiff.blobB.data.data.path,
          location: updatedDiff.location,
        };
        return res;
      }
      const res: IPathDatasetBlobDiff = {
        category: 'code',
        type: 'path',
        diffType: 'updated',
        blobA: updatedDiff.blobA.data.data.path,
        blobB: updatedDiff.blobB.data.data.path,
        location: updatedDiff.location,
      };
      return res as IPathDatasetBlobDiff;
    },
  });
};
export const getGitCodeDiffFromNotebook = (
  diff: INotebookCodeDiff
): IGitCodeDiff | null => {
  return matchBy(diff, 'diffType')({
    added: addedDiff => {
      if (!addedDiff.blob.data.data.gitBlob) {
        return null;
      }
      const res: IGitCodeDiff = {
        category: 'code',
        type: 'git',
        diffType: 'added',
        location: addedDiff.location,
        blob: {
          category: 'code',
          data: addedDiff.blob.data.data.gitBlob,
        },
      };
      return res;
    },
    deleted: deletedDiff => {
      if (!deletedDiff.blob.data.data.gitBlob) {
        return null;
      }
      const res: IGitCodeDiff = {
        category: 'code',
        type: 'git',
        diffType: 'deleted',
        blob: {
          category: 'code',
          data: deletedDiff.blob.data.data.gitBlob,
        },
        location: deletedDiff.location,
      };
      return res;
    },
    updated: updatedDiff => {
      if (
        !updatedDiff.blobA.data.data.gitBlob ||
        !updatedDiff.blobB.data.data.gitBlob
      ) {
        return null;
      }
      if (
        updatedDiff.blobA.data.data.gitBlob &&
        !updatedDiff.blobB.data.data.gitBlob
      ) {
        const res: IGitCodeDiff = {
          category: 'code',
          type: 'git',
          diffType: 'deleted',
          blob: {
            category: 'code',
            data: updatedDiff.blobA.data.data.gitBlob,
          },
          location: updatedDiff.location,
        };
        return res;
      }
      if (
        !updatedDiff.blobA.data.data.gitBlob &&
        updatedDiff.blobB.data.data.gitBlob
      ) {
        const res: IGitCodeDiff = {
          category: 'code',
          type: 'git',
          diffType: 'added',
          blob: {
            category: 'code',
            data: updatedDiff.blobB.data.data.gitBlob,
          },
          location: updatedDiff.location,
        };
        return res;
      }
      const res: IGitCodeDiff = {
        category: 'code',
        type: 'git',
        diffType: 'updated',
        blobA: {
          category: 'code',
          data: updatedDiff.blobA.data.data.gitBlob,
        },
        blobB: {
          category: 'code',
          data: updatedDiff.blobB.data.data.gitBlob,
        },
        location: updatedDiff.location,
      };
      return res as IGitCodeDiff;
    },
  });
};
