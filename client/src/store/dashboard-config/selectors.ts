import { IApplicationState } from '../store';
import { IDashboardConfigState } from './types';

const selectState = (state: IApplicationState): IDashboardConfigState =>
  state.dashboardConfig;

export const selectColumnConfig = (state: IApplicationState) =>
  selectState(state).columnConfig;
