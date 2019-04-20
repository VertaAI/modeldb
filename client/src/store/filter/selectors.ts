import { IApplicationState } from '../store';
import { IFilterState } from './types';

const selectState = (state: IApplicationState): IFilterState => state.filters;

export const selectCurrentContextName = (state: IApplicationState) =>
  selectState(state).data.context;

export const selectCurrentContextData = (state: IApplicationState) => {
  const currentContextName = selectCurrentContextName(state);
  return currentContextName
    ? selectContextsData(state)[currentContextName]
    : undefined;
};

export const selectContextDataByName = (
  state: IApplicationState,
  name: string
) => selectContextsData(state)[name];

export const selectContextsData = (state: IApplicationState) =>
  selectState(state).data.contexts;

export const selectFoundFilters = (state: IApplicationState) =>
  selectState(state).data.foundFilters;
