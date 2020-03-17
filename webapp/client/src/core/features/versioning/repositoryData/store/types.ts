import {
  IFolderElement,
  CommitTag,
  Branch,
  CommitPointer,
  ICommitWithData,
} from 'core/shared/models/Versioning/RepositoryData';
import { ICommunication } from 'core/shared/utils/redux/communication';

export interface IRepositoryDataState {
  data: {
    commitWithData: ICommitWithData | null;

    tags: CommitTag[] | null;

    commitPointer: CommitPointer;

    branches: Branch[];
  };
  communications: {
    loadingCommitWithData: ICommunication;

    loadingTags: ICommunication;

    loadingBranches: ICommunication;
  };
}
