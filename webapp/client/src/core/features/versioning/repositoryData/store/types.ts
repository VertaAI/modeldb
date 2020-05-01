import {
  IFolderElement,
  CommitTag,
  Branch,
  CommitPointer,
  ICommitWithComponent,
} from 'core/shared/models/Versioning/RepositoryData';
import { ICommunication } from 'core/shared/utils/redux/communication';
import { IExperimentRunInfo } from 'models/ModelRecord';

export interface IRepositoryDataState {
  data: {
    commitWithComponent: ICommitWithComponent | null;
    currentBlobExperimentRuns: IExperimentRunInfo[] | null;

    tags: CommitTag[] | null;

    commitPointer: CommitPointer;

    branches: Branch[];
  };
  communications: {
    loadingCommitWithComponent: ICommunication;
    loadingCurrentBlobExperimentRuns: ICommunication;

    loadingTags: ICommunication;

    loadingBranches: ICommunication;
  };
}
