import { makeSelectDeletingEntity } from 'store/shared/deletion';

import { IApplicationState } from '../store';
import { IDatasetVersionsState } from './types';

const selectState = (state: IApplicationState): IDatasetVersionsState =>
  state.datasetVersions;

export const selectDatasetVersions = (state: IApplicationState) =>
  selectState(state).data.datasetVersions;

export const selectDatasetVersion = (state: IApplicationState, id: string) =>
  (selectState(state).data.datasetVersions || []).find(
    datasetVersion => datasetVersion.id === id
  );

export const selectCommunications = (state: IApplicationState) =>
  selectState(state).communications;

export const selectDatasetVersionIdsForDeleting = (
  state: IApplicationState
) => {
  return selectState(state).data.datasetVersionIdsForDeleting;
};

export const selectDeletingDatasetVersion = makeSelectDeletingEntity({
  selectBulkDeleting: state =>
    selectCommunications(state).deletingDatasetVersions,
  selectEntityDeleting: (state, id) =>
    selectState(state).communications.deletingDatasetVersion[id],
  selectEntityIdsForDeleting: selectDatasetVersionIdsForDeleting,
});

export const selectIsSelectedAllDatasetVersionsForDeleting = (
  state: IApplicationState
) => {
  return (
    selectDatasetVersionIdsForDeleting(state).length ===
    (selectDatasetVersions(state) || []).length
  );
};

export const selectDatasetVersionsPagination = (state: IApplicationState) =>
  selectState(state).data.pagination;

export const selectDatasetVersionExperimentRuns = (
  state: IApplicationState,
  datasetVersionId: string
) => selectState(state).data.datasetVersionExperimentRuns[datasetVersionId];
