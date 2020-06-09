import * as CommitComponentLocation from 'core/shared/models/Versioning/CommitComponentLocation';
import {
  IHydratedCommit,
  Branch,
} from 'core/shared/models/Versioning/RepositoryData';

export interface ICommitHistorySettings {
  branch: Branch;
  currentPage: number;
  location: CommitComponentLocation.CommitComponentLocation;
}

export type ICommitView = Pick<
  IHydratedCommit,
  'author' | 'dateCreated' | 'message' | 'sha'
>;

export interface IGroupedCommitsByDate {
  dateCreated: ICommitView['dateCreated'];
  commits: ICommitView[];
}
