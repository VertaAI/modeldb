import { Reducer } from 'redux';
import { filtersActionTypes, IFilterState, initContextAction, searchFiltersAction } from './types';

const filterInitialState: IFilterState = {
  appliedFilters: [],
  context: '',
  foundFilters: [],
  isFiltersSupporting: false
};

type allFilterActions = searchFiltersAction | initContextAction;
export const filtersReducer: Reducer<IFilterState, searchFiltersAction> = (state = filterInitialState, action: allFilterActions) => {
  switch (action.type) {
    case filtersActionTypes.SEARCH_FILTERS_RESULT: {
      return { ...state, foundFilters: action.payload };
    }
    case filtersActionTypes.CHANGE_CONTEXT: {
      return { ...state, context: action.payload };
    }
    case filtersActionTypes.IS_FILTERS_SUPPORT: {
      return { ...state, isFiltersSupporting: action.payload };
    }
    default: {
      return state;
    }
  }
};
