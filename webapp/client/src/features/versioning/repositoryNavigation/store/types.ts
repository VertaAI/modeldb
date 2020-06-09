import * as History from 'shared/utils/History';

type Path = string;

export type RepositoryHistory = History.IHistory<Path>;

export interface IRepositoryNavigationState {
  history: RepositoryHistory | null;
}
