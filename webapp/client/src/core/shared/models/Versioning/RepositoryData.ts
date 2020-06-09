import User from 'core/shared/models/User';

import { Brand } from '../../utils/Brand';
import { IBlob } from './Blob/Blob';
import { CommitComponentLocation } from './CommitComponentLocation';
import { IRepository } from './Repository';

export type SHA = string;

export type CommitPointer =
  | { type: 'tag'; value: CommitTag }
  | { type: 'branch'; value: Branch }
  | { type: 'commitSha'; value: ICommit['sha'] };
export type CommitPointerValue = CommitTag | Branch;
export const CommitPointerHelpers = {
  makeFromTag: (tag: CommitTag | null): CommitPointer => {
    return tag
      ? { type: 'tag', value: tag }
      : { type: 'branch', value: defaultBranch };
  },
  makeFromBranch: (branch: Branch): CommitPointer => {
    return { type: 'branch', value: branch };
  },
  makeFromCommitSha: (sha: ICommit['sha']): CommitPointer => {
    return { type: 'commitSha', value: sha };
  },
  makeCommitPointerFromString: (
    string: string,
    { branches, tags }: { branches: RepositoryBranches; tags: CommitTag[] }
  ): CommitPointer => {
    if (!string) {
      return defaultCommitPointer;
    }
    if (tags.includes(string)) {
      return CommitPointerHelpers.makeFromTag(string!);
    }
    if (branches.includes(string)) {
      return CommitPointerHelpers.makeFromBranch(string!);
    }
    return CommitPointerHelpers.makeFromCommitSha(string);
  },
};
export const defaultBranch = 'master';
export const defaultCommitPointer: CommitPointer = {
  type: 'branch',
  value: defaultBranch,
};

export type CommitTag = string;
export type Branch = string | typeof defaultBranch;
export type RepositoryBranches = Branch[];

export interface ICommit {
  type: 'initial' | 'withParent';
  sha: SHA;
  message: string;
  dateCreated: Date;
  authorId: User['id'];
}
export interface IHydratedCommit {
  type: 'initial' | 'withParent';
  sha: SHA;
  message: string;
  dateCreated: Date;
  author: User;
}

export type ICommitComponent = IFolder | IBlob;

export interface IFolder {
  type: 'folder';
  blobs: IBlobFolderElement[];
  subFolders: ISubFolderElement[];
}
export const emptyFolder: IFolder = {
  type: 'folder',
  blobs: [],
  subFolders: [],
};
export type IFolderElement = IBlobFolderElement | ISubFolderElement;
declare const FolderElementNameSymbol: unique symbol;
export type FolderElementName = Brand<
  string,
  'folderElementName',
  typeof FolderElementNameSymbol
>;
export interface IBlobFolderElement {
  type: 'blob';
  name: FolderElementName;
}
export interface ISubFolderElement {
  type: 'folder';
  name: FolderElementName;
}

export interface ICommitComponentRequest {
  repositoryId: IRepository['id'];
  fullCommitComponentLocationComponents: IFullCommitComponentLocationComponents;
}

export interface IFullCommitComponentLocationComponents {
  type: ICommitComponent['type'];
  location: CommitComponentLocation;
  commitPointer: CommitPointer;
}

export interface ICommitWithComponent {
  commit: IHydratedCommit;
  component: ICommitComponent;
}

export type IMergeCommitsError = {
  type: 'mergeCommitsError';
};
export const isMergeCommitsError = (
  error: any
): error is IMergeCommitsError => {
  const expectedType: IMergeCommitsError['type'] = 'mergeCommitsError';
  return error && error.type === expectedType;
};
