import * as History from 'core/shared/utils/History';

type Path = string;

export type RepositoryHistory = History.IHistory<Path>;

export interface IRepositoryNavigationState {
  history: RepositoryHistory | null;
}
