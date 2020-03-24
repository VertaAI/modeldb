import { DataWithPagination } from 'core/shared/models/Pagination';
import * as DataLocation from 'core/shared/models/Versioning/DataLocation';
import {
  IHydratedCommit,
  Branch,
} from 'core/shared/models/Versioning/RepositoryData';
import { ICommunication } from 'core/shared/utils/redux/communication';

export interface ICommitsHistoryState {
  data: {
    commitsWithPagination: DataWithPagination<IHydratedCommit> | null;
  };
  communications: {
    loadingCommits: ICommunication;
  };
}

export interface ICommitHistorySettings {
  branch: Branch;
  currentPage: number;
  location: DataLocation.DataLocation;
}

export interface IGroupedCommitsByDate {
  dateCreated: IHydratedCommit['dateCreated'];
  commits: IHydratedCommit[];
}
