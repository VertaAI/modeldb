import { IApplicationState } from 'store/store';

const selectFeatureState = (state: IApplicationState) => state.repositoryData;

export const selectCommitWithData = (state: IApplicationState) =>
  selectFeatureState(state).data.commitWithData;

export const selectCommunications = (state: IApplicationState) =>
  selectFeatureState(state).communications;

export const selectTags = (state: IApplicationState) =>
  selectFeatureState(state).data.tags;

export const selectCommitPointer = (state: IApplicationState) =>
  selectFeatureState(state).data.commitPointer;

export const selectBranches = (state: IApplicationState) =>
  selectFeatureState(state).data.branches;
