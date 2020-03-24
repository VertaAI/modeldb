import { IApplicationState } from 'store/store';

import { ICommitsHistoryState } from './types';

export const selectState = (state: IApplicationState): ICommitsHistoryState => {
  return state.commitsHistory;
};

export const selectCommitsWithPagination = (state: IApplicationState) => {
  return selectState(state).data.commitsWithPagination;
};

export const selectCommunications = (state: IApplicationState) =>
  selectState(state).communications;
