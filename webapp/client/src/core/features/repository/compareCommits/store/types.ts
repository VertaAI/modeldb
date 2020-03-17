import { Diff } from 'core/shared/models/Versioning/Blob/Diff';
import { ICommunication } from 'core/shared/utils/redux/communication';

export interface ICompareCommitsState {
  data: {
    diffs: Diff[] | null;
  };
  communications: {
    loadingCommitsDiff: ICommunication;
  };
}
