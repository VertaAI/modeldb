import { IFilterData } from 'components/FilterSelect/FilterSelect';
import { Reducer } from 'redux';
import { HashMap } from '../../types/HashMap';
import {
  filtersActionTypes,
  IFilterContextData,
  IFilterState,
  initContextAction,
  manageFiltersAction,
  manageFiltersTypes,
  searchFiltersAction
} from './types';

const filterInitialState: IFilterState = {
  contexts: new HashMap<IFilterContextData>()
};

type allFilterActions = searchFiltersAction | initContextAction | manageFiltersAction;
export const filtersReducer: Reducer<IFilterState, searchFiltersAction> = (state = filterInitialState, action: allFilterActions) => {
  switch (action.type) {
    case filtersActionTypes.SEARCH_FILTERS_RESULT: {
      return { ...state, foundFilters: action.payload };
    }
    case filtersActionTypes.CHANGE_CONTEXT: {
      const contexts: HashMap<IFilterContextData> = state.contexts;
      if (contexts.has(action.payload)) {
        return { ...state, context: action.payload };
      }
      return { ...state, context: undefined };
    }
    case filtersActionTypes.REGISTER_CONTEXT: {
      const data: IFilterContextData = action.payload;
      if (!state.contexts.has(data.ctx)) {
        const newMap: HashMap<IFilterContextData> = new HashMap<IFilterContextData>(state.contexts);
        newMap.set(data.ctx, data);
        return { ...state, contexts: newMap };
      }
      return state;
    }
    case manageFiltersTypes.ADD_FILTER: {
      if (state.context === undefined || !state.contexts.has(state.context)) {
        throw new Error('Current filters context is undefined');
      }

      const newState: IFilterState = { ...state };
      const currentCtx: IFilterContextData = newState.contexts.get(state.context);
      currentCtx.appliedFilters = [...currentCtx.appliedFilters, action.payload];
      newState.contexts.set(state.context, currentCtx);
      return newState;
    }
    case manageFiltersTypes.REMOVE_FILTER: {
      if (state.context === undefined || !state.contexts.has(state.context)) {
        throw new Error('Current filters context is undefined');
      }

      const newState: IFilterState = { ...state };
      const currentCtx: IFilterContextData = newState.contexts.get(state.context);
      currentCtx.appliedFilters = [...currentCtx.appliedFilters];
      currentCtx.appliedFilters.splice(currentCtx.appliedFilters.indexOf(action.payload), 1);
      newState.contexts.set(state.context, currentCtx);
      return newState;
    }
    default: {
      return state;
    }
  }
};
