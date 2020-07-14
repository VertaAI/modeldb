import {
  IBlobFolderElement,
  ISubFolderElement,
  IHydratedCommit,
} from 'shared/models/Versioning/RepositoryData';
import { convertServerBlobToClient } from 'services/serverModel/Versioning/RepositoryData/Blob';
import { convertUser } from 'shared/graphql/User/User';

import { ICommitComponentView } from '../types';
import {
  CommitWithComponent_repository_commitByReference_getLocation,
  CommitWithComponent_repository_commitByReference,
} from './graphql-types/CommitWithComponent';

export const convertCommitWithComponent = (
  serverCommit: CommitWithComponent_repository_commitByReference
): { commit: IHydratedCommit; component: ICommitComponentView } => {
  const component = convertComponent(serverCommit.getLocation);
  return {
    commit: {
      dateCreated: new Date(Number(serverCommit.date)),
      message: serverCommit.message,
      sha: serverCommit.id,
      author: convertUser(serverCommit.author),
      type: component ? 'withParent' : 'initial',
    },
    component,
  };
};

export const convertComponent = (
  serverCommitComponent: CommitWithComponent_repository_commitByReference_getLocation | null
): ICommitComponentView => {
  if (!serverCommitComponent) {
    return { type: 'folder', blobs: [], subFolders: [] };
  }
  switch (serverCommitComponent.__typename) {
    case 'CommitBlob': {
      const blob = convertServerBlobToClient(
        JSON.parse(serverCommitComponent.content)
      );
      return {
        type: 'blob',
        data: blob.data,
        experimentRuns: serverCommitComponent.runs.runs,
      };
    }
    case 'CommitFolder': {
      return {
        type: 'folder',
        blobs: serverCommitComponent.blobs.map(({ name }) => {
          const res: IBlobFolderElement = {
            type: 'blob',
            name: name as IBlobFolderElement['name'],
          };
          return res;
        }),
        subFolders: serverCommitComponent.subfolders.map(({ name }) => {
          const res: ISubFolderElement = {
            type: 'folder',
            name: name as ISubFolderElement['name'],
          };
          return res;
        }),
      };
    }
  }
};
