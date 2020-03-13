import { makeSelectDeletingEntity } from 'store/shared/deletion';

import { initialCommunication } from 'core/shared/utils/redux/communication';
import { IApplicationState } from '../store';
import { IDatasetsState } from './types';

const selectState = (state: IApplicationState): IDatasetsState =>
  state.datasets;

export const selectDatasets = (state: IApplicationState) =>
  selectState(state).data.datasets;

export const selectCommunications = (state: IApplicationState) =>
  selectState(state).communications;

export const selectDatasetIdsForDeleting = (state: IApplicationState) => {
  return selectState(state).data.datasetIdsForDeleting;
};

export const selectDeletingDataset = makeSelectDeletingEntity({
  selectEntityIdsForDeleting: selectDatasetIdsForDeleting,
  selectBulkDeleting: state => selectCommunications(state).deletingDatasets,
  selectEntityDeleting: (state, id) =>
    selectState(state).communications.deletingDataset[id],
});

export const selectDataset = (state: IApplicationState, id: string) => {
  return (selectDatasets(state) || []).find(dataset => dataset.id === id);
};

export const selectDatasetsPagination = (state: IApplicationState) =>
  selectState(state).data.pagination;

export const selectLoadingDataset = (state: IApplicationState, id: string) => {
  if (selectDataset(state, id)) {
    return { error: undefined, isRequesting: false, isSuccess: true };
  }
  return selectCommunications(state).loadingDataset[id] || initialCommunication;
};
