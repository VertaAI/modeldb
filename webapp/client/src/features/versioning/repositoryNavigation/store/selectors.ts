import { IApplicationState } from 'setup/store/store';
import * as History from 'shared/utils/History';

import { IRepositoryNavigationState } from './types';

export const selectState = (
  state: IApplicationState
): IRepositoryNavigationState => {
  return state.repositoryNavigation;
};

export const isBackEnabled = (state: IApplicationState) => {
  const history = selectState(state).history;
  return Boolean(history && History.isBackEnabled(history));
};

export const isForwardEnabled = (state: IApplicationState) => {
  const history = selectState(state).history;
  return Boolean(history && History.isForwardEnabled(history));
};
