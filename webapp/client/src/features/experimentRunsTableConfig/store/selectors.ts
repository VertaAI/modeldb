import {
  IExperimentRunsTableConfigRootState,
  IExperimentRunsTableConfigState,
} from './types';

const selectState = (
  state: IExperimentRunsTableConfigRootState
): IExperimentRunsTableConfigState => state.experimentRunsTableConfig;

export const selectColumnConfig = (
  state: IExperimentRunsTableConfigRootState
) => selectState(state).columnConfig;
