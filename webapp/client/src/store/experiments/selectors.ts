import { makeSelectDeletingEntity } from 'store/shared/deletion';

import { IApplicationState } from '../store';
import { IExperimentsState } from './types';

const selectState = (state: IApplicationState): IExperimentsState =>
  state.experiments;

export const selectExperiments = (state: IApplicationState) =>
  selectState(state).data.experiments;

export const selectExperiment = (state: IApplicationState, id: string) =>
  (selectState(state).data.experiments || []).find(exp => exp.id === id);

export const selectLoadingExperiments = (state: IApplicationState) =>
  selectState(state).communications.loadingExpriments;

export const selectExperimentIdsForDeleting = (state: IApplicationState) => {
  return selectState(state).data.experimentIdsForDeleting;
};

export const selectDeletingExperiment = makeSelectDeletingEntity({
  selectBulkDeleting: state =>
    selectState(state).communications.deletingExperiments,
  selectEntityDeleting: (state, id) =>
    selectState(state).communications.deletingExperiment[id],
  selectEntityIdsForDeleting: selectExperimentIdsForDeleting,
});

export const selectExperimentsPagination = (state: IApplicationState) => {
  return selectState(state).data.pagination;
};

export const selectCommunications = (state: IApplicationState) => {
  return selectState(state).communications;
};
