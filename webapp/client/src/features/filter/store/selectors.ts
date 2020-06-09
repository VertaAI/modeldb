import { PropertyType } from 'shared/models/Filters';

import { IFilterRootState, IFilterState } from './types';

const selectState = (state: IFilterRootState): IFilterState => state.filters;

export const selectCurrentContextAppliedFilters = (state: IFilterRootState) => {
  return selectCurrentContextFilters(state);
};

export const selectContextFilters = (state: IFilterRootState, name: string) => {
  const context = selectContextDataByName(state, name);
  return context ? context.filters : [];
};

export const selectCurrentContextFilters = (state: IFilterRootState) => {
  const context = selectCurrentContextData(state);
  return context ? context.filters : [];
};

export const selectCurrentContextName = (state: IFilterRootState) =>
  selectState(state).data.currentContextName;

export const selectCurrentContextData = (state: IFilterRootState) => {
  const currentContextName = selectCurrentContextName(state);
  return currentContextName
    ? selectContextsData(state)[currentContextName]
    : undefined;
};

export const selectContextDataByName = (
  state: IFilterRootState,
  name: string
) => selectContextsData(state)[name];

export const selectContextsData = (state: IFilterRootState) =>
  selectState(state).data.contexts;

export const hasContext = (state: IFilterRootState, name: string) =>
  Boolean(selectContextDataByName(state, name));

export const selectCurrentContextFiltersByType = (
  state: IFilterRootState,
  type: PropertyType
) => {
  const context = selectCurrentContextData(state);
  return context ? context.filters.filter(filter => filter.type === type) : [];
};
