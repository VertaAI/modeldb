import { Reducer } from 'redux';
import { IFilterData } from '../../models/Filters';

import {
  IFilterContextData,
  IFilterState,
  initActionTypes,
  initContextAction,
  manageFiltersAction,
  manageFiltersTypes,
  suggestFiltersAction,
  suggestFiltersActionTypes
} from './types';

const filterInitialState: IFilterState = {
  contexts: {}
};

type allFilterActions = suggestFiltersAction | initContextAction | manageFiltersAction;

const manageFiltersReducer: Reducer<IFilterState, manageFiltersAction> = (state = filterInitialState, action: manageFiltersAction) => {
  const ctx = action.payload.ctx;
  if (ctx === undefined || state.contexts[ctx] === undefined) {
    throw new Error('Current filters context is undefined');
  }

  switch (action.type) {
    case manageFiltersTypes.ADD_FILTER: {
      const newState: IFilterState = { ...state };
      const currentCtx: IFilterContextData | undefined = newState.contexts[ctx];
      if (currentCtx !== undefined) {
        currentCtx.appliedFilters = [...currentCtx.appliedFilters, action.payload.filter];
        newState.contexts[ctx] = currentCtx;
        return newState;
      }
    }
    case manageFiltersTypes.REMOVE_FILTER: {
      const newState: IFilterState = { ...state };
      const currentCtx: IFilterContextData | undefined = newState.contexts[ctx];
      if (currentCtx !== undefined) {
        currentCtx.appliedFilters = [...currentCtx.appliedFilters];
        currentCtx.appliedFilters.splice(currentCtx.appliedFilters.indexOf(action.payload.filter), 1);
        newState.contexts[ctx] = currentCtx;
        return newState;
      }
    }
    case manageFiltersTypes.EDIT_FILTER: {
      const newState = { ...state };
      const ctxData = newState.contexts[ctx];
      if (ctx !== undefined && action.payload.index !== undefined) {
        const newAppliedFilters = Array<IFilterData>();

        newAppliedFilters.push(
          ...ctxData.appliedFilters.slice(0, action.payload.index - 1),
          action.payload.filter,
          ...ctxData.appliedFilters.slice(action.payload.index + 1)
        );
        ctxData.appliedFilters = newAppliedFilters;
        newState.contexts[ctx] = ctxData;
        return newState;
      }
    }
    default: {
      return state;
    }
  }
};

const initReducer: Reducer<IFilterState, initContextAction> = (state = filterInitialState, action: initContextAction) => {
  switch (action.type) {
    case initActionTypes.REGISTER_CONTEXT_SUCCESS: {
      const data: IFilterContextData[] = action.payload;
      const newMap: { [index: string]: IFilterContextData } = {};
      for (const ctxData of data) {
        newMap[ctxData.ctx] = ctxData;
      }

      return { ...state, contexts: newMap };
    }
    case initActionTypes.CHANGE_CONTEXT: {
      return { ...state, context: action.payload };
      // const contexts = state.contexts;
      // if (contexts[action.payload] !== undefined) {
      //   return { ...state, context: action.payload };
      // }
      // return { ...state, context: undefined };
    }
    default: {
      return state;
    }
  }
};

const suggestReducer: Reducer<IFilterState, suggestFiltersAction> = (state = filterInitialState, action: suggestFiltersAction) => {
  switch (action.type) {
    case suggestFiltersActionTypes.SUGGEST_FILTERS_RESULT: {
      return { ...state, foundFilters: action.payload };
    }
    default: {
      return state;
    }
  }
};

export const filtersReducer: Reducer<IFilterState, allFilterActions> = (state = filterInitialState, action: allFilterActions) => {
  if (Object.values(manageFiltersTypes).includes(action.type)) {
    return manageFiltersReducer(state, action as manageFiltersAction);
  }
  if (Object.values(suggestFiltersActionTypes).includes(action.type)) {
    return suggestReducer(state, action as suggestFiltersAction);
  }
  if (Object.values(initActionTypes).includes(action.type)) {
    return initReducer(state, action as initContextAction);
  }
  return state;
};
