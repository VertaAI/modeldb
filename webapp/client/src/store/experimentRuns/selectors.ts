import { makeSelectDeletingEntity } from 'store/shared/deletion';

import { IApplicationState } from '../store';
import { IExperimentRunsState } from './types';

const selectState = (state: IApplicationState): IExperimentRunsState =>
  state.experimentRuns;

export const selectExperimentRuns = (state: IApplicationState) =>
  selectState(state).data.modelRecords;

export const selectLoadingExperimentRuns = (state: IApplicationState) =>
  selectState(state).communications.loadingExperimentRuns;

export const selectSequentialChartData = (state: IApplicationState) =>
  selectState(state).data.sequentialChartData;

export const selectLazyChartData = (state: IApplicationState) =>
  selectState(state).data.lazyChartData;

export const selectLoadingSequentialChartData = (state: IApplicationState) =>
  selectState(state).communications.loadingSequentialChartData;

export const selectLoadingLazyChartData = (state: IApplicationState) =>
  selectState(state).communications.loadingLazyChartData;

export const selectExperimentRunsPagination = (state: IApplicationState) =>
  selectState(state).data.pagination;

export const selectExperimentRunsSorting = (state: IApplicationState) =>
  selectState(state).data.sorting;

export const selectIsLoadingExperimentRun = (
  state: IApplicationState,
  id: string
) => {
  const comm = selectState(state).communications.loadingExperimentRun[id];
  return Boolean(comm && comm.isRequesting);
};

export const selectExperimentRun = (state: IApplicationState, id: string) => {
  return (selectExperimentRuns(state) || []).find(x => x.id === id) || null;
};

export const selectExperimentRunIdsForDeleting = (state: IApplicationState) => {
  return selectState(state).data.modelRecordIdsForDeleting;
};
export const selectIsSelectedAllExperimentRunsForDeleting = (
  state: IApplicationState
) => {
  return (
    selectExperimentRunIdsForDeleting(state).length ===
    (selectExperimentRuns(state) || []).length
  );
};

export const selectDeletingExperimentRun = makeSelectDeletingEntity({
  selectBulkDeleting: state =>
    selectCommunications(state).deletingExperimentRuns,
  selectEntityDeleting: (state, id) =>
    selectCommunications(state).deletingExperimentRun[id],
  selectEntityIdsForDeleting: selectExperimentRunIdsForDeleting,
});

export const selectDeletingExperimentRunArtifacts = (
  state: IApplicationState
) => {
  return selectState(state).communications.deletingExperimentRunArtifact;
};

export const selectCommunications = (state: IApplicationState) => {
  return selectState(state).communications;
};
