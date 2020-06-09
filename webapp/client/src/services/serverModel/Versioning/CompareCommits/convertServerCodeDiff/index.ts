import {
  IGitCodeDiff,
  ICodeBlobDiff,
  INotebookCodeDiff,
} from 'core/shared/models/Versioning/Blob/CodeBlob';

import { convertServerGitBlob } from '../../RepositoryData/Blob/CodeBlob';
import {
  IServerBlobDiff,
  IServerElementDiff,
  convertServerElementDiffToClient,
  convertServerBlobDiffToClient,
  convertNullableServerElementDiffToClient,
} from '../ServerDiff';
import { convertServerDatasetPathComponent } from '../../RepositoryData/Blob/DatasetBlob';
import { IServerPathDatasetComponentBlobDiff } from '../convertServerDatasetDiff';

export const convertServerCodeDiff = (
  serverCodeDiff: IServerCodeBlobDiff
): ICodeBlobDiff => {
  if (serverCodeDiff.code.git) {
    return convertServerBlobDiffToClient<IServerCodeBlobDiff, IGitCodeDiff>(
      {
        convertData: () =>
          convertServerElementDiffToClient(
            convertServerGitBlob,
            serverCodeDiff.code.git!
          ),
        category: 'code',
        type: 'git',
      },
      serverCodeDiff
    );
  }

  if (serverCodeDiff.code.notebook) {
    return convertServerBlobDiffToClient<
      IServerCodeBlobDiff,
      INotebookCodeDiff
    >(
      {
        convertData: () => {
          const serverNotebook = serverCodeDiff.code.notebook!;
          return {
            git: convertNullableServerElementDiffToClient(
              convertServerGitBlob,
              serverNotebook.git_repo
            ),
            path: convertNullableServerElementDiffToClient(
              convertServerDatasetPathComponent,
              serverNotebook.path
            ),
          };
        },
        category: 'code',
        type: 'notebook',
      },
      serverCodeDiff
    );
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
  };
}>;

type IServerGitDiff = IServerElementDiff<{
  repo: string;
  hash?: string;
  branch?: string;
  tag?: string;
  is_dirty: boolean;
}>;
