import { IApplicationState } from 'store/store';
import { ICompareChangesState } from './types';

export const selectFeatureState = (
  state: IApplicationState
): ICompareChangesState => state.compareChanges;

export const selectCommunications = (state: IApplicationState) =>
  selectFeatureState(state).communications;

export const selectCommitPointersCommits = (state: IApplicationState) =>
  selectFeatureState(state).data.commitPointersCommits;
