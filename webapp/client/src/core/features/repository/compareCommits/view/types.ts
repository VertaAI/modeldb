import { ICommit } from 'core/shared/models/Versioning/RepositoryData';

export interface IComparedCommitsInfo {
  commitA: Pick<ICommit, 'sha'>;
  commitB: Pick<ICommit, 'sha'>;
}
