import {
  IBlobFolderElement,
  IFolder,
  ISubFolderElement,
  ICommit,
} from 'core/shared/models/Repository/RepositoryData';

export const convertServerFolderToClient = (serverFolder: any): IFolder => {
  return {
    type: 'folder',
    blobs: (serverFolder.blobs || []).map(
      ({ element_name, created_by_commit }: any) => {
        const res: IBlobFolderElement = {
          type: 'blob',
          name: element_name,
          createdByCommitSha: created_by_commit,
        };
        return res;
      }
    ),
    subFolders: (serverFolder.sub_folders || []).map(
      ({ element_name, created_by_commit }: any) => {
        const res: ISubFolderElement = {
          type: 'folder',
          name: element_name,
          createdByCommitSha: created_by_commit,
        };
        return res;
      }
    ),
  };
};

export const convertServerCommitToClient = (serverCommit: any): ICommit => {
  return {
    authorId: serverCommit.author,
    dateCreated: new Date(Number(serverCommit.date_created)),
    sha: serverCommit.commit_sha,
    message: serverCommit.message,
    parentShas: serverCommit.parent_shas || [],
    type: serverCommit.parent_shas ? 'withParent' : 'initial',
  };
};
