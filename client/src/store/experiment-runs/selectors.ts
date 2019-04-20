import { IApplicationState } from '../store';
import { IExperimentRunsState } from './types';

const selectState = (state: IApplicationState): IExperimentRunsState =>
  state.experimentRuns;

export const selectExperimentRuns = (state: IApplicationState) =>
  selectState(state).data.modelRecords;

export const selectIsLoadingExperimentRuns = (state: IApplicationState) =>
  selectState(state).communications.loadingExperimentRuns.isRequesting;
