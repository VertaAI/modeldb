import { ICommit } from 'core/shared/models/Repository/RepositoryData';

export interface IComparedCommitsInfo {
  commitA: Pick<ICommit, 'sha'>;
  commitB: Pick<ICommit, 'sha'>;
}
