import {
  IGitCodeDiff,
  ICodeBlobDiff,
  INotebookCodeDiff,
} from 'core/shared/models/Versioning/Blob/CodeBlob';
import { exhaustiveCheck } from 'core/shared/utils/exhaustiveCheck';
import { DataLocation } from 'core/shared/models/Versioning/DataLocation';

import {
  convertServerGitBlob,
} from '../../RepositoryData/Blob/CodeBlob';
import { IServerBlobDiff, IServerElementDiff, convertServerElementDiffToClient } from '../ServerDiff';
import { convertServerDatasetPathComponent } from '../../RepositoryData/Blob/DatasetBlob';
import { IServerPathDatasetComponentBlobDiff } from '../convertServerDatasetDiff';

export const convertServerCodeDiff = (
  serverCodeDiff: IServerCodeBlobDiff,
): ICodeBlobDiff => {
  const { location } = serverCodeDiff;
  if (serverCodeDiff.code.git) {
    switch (serverCodeDiff.code.git.status) {
      case 'ADDED': {
        const diff: IGitCodeDiff = {
          diffType: 'added' as const,
          category: 'code',
          type: 'git',
          location: location as DataLocation,
          data: convertServerElementDiffToClient(convertServerGitBlob, serverCodeDiff.code.git),
        };
        return diff;
      }
      case 'DELETED': {
        const diff: IGitCodeDiff = {
          diffType: 'deleted' as const,
          category: 'code',
          type: 'git',
          location: location as DataLocation,
          data: convertServerElementDiffToClient(convertServerGitBlob, serverCodeDiff.code.git),
        };
        return diff;
      }
      case 'MODIFIED': {
        const diff: IGitCodeDiff = {
          diffType: 'updated',
          type: 'git',
          category: 'code',
          location: location as DataLocation,
          data: convertServerElementDiffToClient(convertServerGitBlob, serverCodeDiff.code.git),
        };
        return diff;
      }
      default:
        return exhaustiveCheck(serverCodeDiff.code.git, '');
    }
  }

  if (serverCodeDiff.code.notebook) {
    const serverNotebook = serverCodeDiff.code.notebook;
    switch (serverCodeDiff.status) {
      case 'ADDED': {
        const diff: INotebookCodeDiff = {
          diffType: 'added' as const,
          category: 'code',
          type: 'notebook',
          location: location as DataLocation,
          data: {
            git: serverNotebook.git_repo ? convertServerElementDiffToClient(convertServerGitBlob, serverNotebook.git_repo) : undefined,
            path: serverNotebook.path ? convertServerElementDiffToClient(convertServerDatasetPathComponent, serverNotebook.path) : undefined,
          },
        };
        return diff;
      }
      case 'DELETED': {
        const diff: INotebookCodeDiff = {
          diffType: 'deleted' as const,
          category: 'code',
          type: 'notebook',
          location: location as DataLocation,
          data: {
            git: serverNotebook.git_repo ? convertServerElementDiffToClient(convertServerGitBlob, serverNotebook.git_repo) : undefined,
            path: serverNotebook.path ? convertServerElementDiffToClient(convertServerDatasetPathComponent, serverNotebook.path) : undefined,
          },
        };
        return diff;
      }
      case 'MODIFIED': {
        const diff: INotebookCodeDiff = {
          diffType: 'updated' as const,
          category: 'code',
          type: 'notebook',
          location: location as DataLocation,
          data: {
            git: serverNotebook.git_repo ? convertServerElementDiffToClient(convertServerGitBlob, serverNotebook.git_repo) : undefined,
            path: serverNotebook.path ? convertServerElementDiffToClient(convertServerDatasetPathComponent, serverNotebook.path) : undefined,
          },
        };
        return diff;
      }
      default:
        return exhaustiveCheck(serverCodeDiff.status, '');
    }
  }

  throw new Error('unknown code diff');
};

export type IServerCodeBlobDiff = IServerBlobDiff<{
  code: {
    git?: IServerGitDiff;
    notebook?: {
      path?: IServerPathDatasetComponentBlobDiff;
      git_repo?: IServerGitDiff;
    };
  }
}>;

type IServerGitDiff = IServerElementDiff<{
  repo: string;
  hash?: string;
  branch?: string;
  tag?: string;
  is_dirty: boolean;
}>;
