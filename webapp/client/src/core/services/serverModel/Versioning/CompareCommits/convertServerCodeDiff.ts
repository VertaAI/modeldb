import {
  IGitCodeDiff,
  ICodeBlobDiff,
  INotebookCodeDiff,
} from 'core/shared/models/Versioning/Blob/CodeBlob';
import { Diff, DiffType } from 'core/shared/models/Versioning/Blob/Diff';
import { exhaustiveCheck } from 'core/shared/utils/exhaustiveCheck';
import {
  convertServerGitBlob,
  convertServerNotebookBlob,
} from '../RepositoryData/Blob/CodeBlob';

export const convertServerCodeDiff = (
  serverCodeDiff: any,
  diffType: DiffType
): ICodeBlobDiff => {
  const category = 'code';
  const { location } = serverCodeDiff;
  if (serverCodeDiff[category].git) {
    switch (diffType) {
      case 'added':
      case 'deleted': {
        const diff: Extract<IGitCodeDiff, { diffType: 'added' | 'deleted' }> = {
          diffType,
          category: 'code',
          type: 'git',
          location,
          blob: {
            category: 'code',
            data: convertServerGitBlob(
              serverCodeDiff[category].git.A || serverCodeDiff[category].git.B
            ),
          },
        } as any;
        return diff;
      }
      case 'updated': {
        const diff: Extract<IGitCodeDiff, { diffType: 'updated' }> = {
          diffType,
          type: 'git',
          category: 'code',
          location,
          blobA: {
            category: 'code',
            data: convertServerGitBlob(serverCodeDiff[category].git.A),
          },
          blobB: {
            category: 'code',
            data: convertServerGitBlob(serverCodeDiff[category].git.B),
          },
        };
        return diff;
      }
      default:
        return exhaustiveCheck(diffType, '');
    }
  }

  if (serverCodeDiff[category].notebook) {
    return convertServerNotebookCodeDiff(
      serverCodeDiff[category].notebook,
      location,
      diffType
    );
  }

  throw new Error('unknown code diff');
};

const convertServerNotebookCodeDiff = (
  serverNotebook: any,
  location: any,
  diffType: DiffType
): INotebookCodeDiff => {
  switch (diffType) {
    case 'added':
    case 'deleted': {
      return {
        type: 'notebook',
        category: 'code',
        diffType: 'added',
        location,
        blob: {
          category: 'code',
          data: convertServerNotebookBlob(serverNotebook.A || serverNotebook.B),
        },
      };
    }
    case 'updated': {
      return {
        type: 'notebook',
        category: 'code',
        diffType: 'updated',
        location,
        blobA: {
          category: 'code',
          data: convertServerNotebookBlob(serverNotebook.A),
        },
        blobB: {
          category: 'code',
          data: convertServerNotebookBlob(serverNotebook.B),
        },
      };
    }
    default:
      return exhaustiveCheck(diffType, '');
  }
};
