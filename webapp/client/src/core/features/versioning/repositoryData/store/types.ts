import {
  IFolderElement,
  CommitTag,
  Branch,
  CommitPointer,
  ICommitWithData,
} from 'core/shared/models/Versioning/RepositoryData';
import { ICommunication } from 'core/shared/utils/redux/communication';
import { IExperimentRunInfo } from 'models/ModelRecord';

export interface IRepositoryDataState {
  data: {
    commitWithData: ICommitWithData | null;
    currentBlobExperimentRuns: IExperimentRunInfo[] | null;

    tags: CommitTag[] | null;

    commitPointer: CommitPointer;

    branches: Branch[];
  };
  communications: {
    loadingCommitWithData: ICommunication;
    loadingCurrentBlobExperimentRuns: ICommunication;

    loadingTags: ICommunication;

    loadingBranches: ICommunication;
  };
}
