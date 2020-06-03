import { IApplicationState } from 'store/store';

const selectFeatureState = (state: IApplicationState) => state.repositoryData;

export const selectCommitWithComponent = (state: IApplicationState) =>
  selectFeatureState(state).data.commitWithComponent;

export const selectCommunications = (state: IApplicationState) =>
  selectFeatureState(state).communications;

export const selectTags = (state: IApplicationState) =>
  selectFeatureState(state).data.tags;

export const selectCommitPointer = (state: IApplicationState) =>
  selectFeatureState(state).data.commitPointer;

export const selectBranches = (state: IApplicationState) =>
  selectFeatureState(state).data.branches;

export const selectCurrentBlobExperimentRuns = (state: IApplicationState) =>
  selectFeatureState(state).data.currentBlobExperimentRuns;
