import { IApplicationState } from 'store/store';

const selectState = (state: IApplicationState) => state.viewCommit;

export const selectCommunications = (state: IApplicationState) =>
  selectState(state).communications;

export const selectCommit = (state: IApplicationState) =>
  selectState(state).data;
