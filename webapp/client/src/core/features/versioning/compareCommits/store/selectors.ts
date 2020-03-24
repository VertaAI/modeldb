import { IApplicationState } from 'store/store';
import { ICompareCommitsState } from './types';

export const selectFeatureState = (
  state: IApplicationState
): ICompareCommitsState => state.compareCommits;

export const selectCommunications = (state: IApplicationState) =>
  selectFeatureState(state).communications;

export const selectDiff = (state: IApplicationState) =>
  selectFeatureState(state).data.diffs;
