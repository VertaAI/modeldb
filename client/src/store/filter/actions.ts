import { FilterContextPool } from 'models/FilterContextPool';
import { IFilterData } from 'models/Filters';
import ServiceFactory from 'services/ServiceFactory';
import { ActionResult } from 'store/store';
import { action } from 'typesafe-actions';
import {
  applyFiltersAction,
  IFilterContextData,
  initActionTypes,
  initContextAction,
  manageFiltersAction,
  manageFiltersTypes,
  suggestFiltersAction,
  suggestFiltersActionTypes
} from './types';

export function suggestFilters(searchString: string): ActionResult<void, suggestFiltersAction> {
  return async (dispatch, getState) => {
    dispatch(action(suggestFiltersActionTypes.SUGGEST_FILTERS_REQUEST));
    const svc = ServiceFactory.getSearchAndFiltersService();
    if (svc != null && getState().filters.context !== undefined) {
      const ctxName = getState().filters.context;
      let ctx: IFilterContextData | undefined;
      if (ctxName !== undefined) {
        ctx = getState().filters.contexts[ctxName];
      }
      await svc.searchFilterSuggestions(searchString, ctx).then(res => {
        dispatch(action(suggestFiltersActionTypes.SUGGEST_FILTERS_RESULT, res));
      });
    }
  };
}

export function applyFilters(ctxName: string, filters: IFilterData[]): ActionResult<void, applyFiltersAction> {
  return async (dispatch, getState) => {
    const ctx = FilterContextPool.getContextByName(ctxName);
    ctx.onApplyFilters(filters, dispatch);
  };
}

export function search(ctxName: string, searchStr: string): ActionResult<void, applyFiltersAction> {
  return async (dispatch, getState) => {
    const ctx = FilterContextPool.getContextByName(ctxName);
    ctx.onSearch(searchStr, dispatch);
  };
}

export function initContexts(): ActionResult<void, initContextAction> {
  return async (dispatch, getState) => {
    dispatch(action(initActionTypes.REGISTER_CONTEXT_SUCCESS, FilterContextPool.initContextsData()));
  };
}

export function changeContext(ctx?: string): ActionResult<void, initContextAction | suggestFiltersAction> {
  return async (dispatch, getState) => {
    dispatch(action(initActionTypes.CHANGE_CONTEXT, ctx));
    dispatch(action(suggestFiltersActionTypes.SUGGEST_FILTERS_RESULT, []));
    if (ctx !== undefined && FilterContextPool.hasContext(ctx)) {
      const ctxData = FilterContextPool.getContextByName(ctx);
      ctxData.onApplyFilters(getState().filters.contexts[ctx].appliedFilters, dispatch);
    }
  };
}

function saveFilters(ctx: string, filters: IFilterData[]) {
  localStorage.setItem(`${ctx}_filter`, JSON.stringify(filters));
}
export function addFilter(filter: IFilterData, ctx: string): ActionResult<void, manageFiltersAction> {
  return async (dispatch, getState) => {
    dispatch(action(manageFiltersTypes.ADD_FILTER, { filter, ctx }));
    saveFilters(ctx, getState().filters.contexts[ctx].appliedFilters);
  };
}

export function editFilter(index: number, filter: IFilterData, ctx: string): ActionResult<void, manageFiltersAction> {
  return async (dispatch, getState) => {
    dispatch(action(manageFiltersTypes.EDIT_FILTER, { index, filter, ctx }));
    saveFilters(ctx, getState().filters.contexts[ctx].appliedFilters);
  };
}

export function removeFilter(filter: IFilterData, ctx: string): ActionResult<void, manageFiltersAction> {
  return async (dispatch, getState) => {
    dispatch(action(manageFiltersTypes.REMOVE_FILTER, { filter, ctx }));
    saveFilters(ctx, getState().filters.contexts[ctx].appliedFilters);
  };
}
