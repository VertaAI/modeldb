import { action } from 'typesafe-actions';

import { FilterContextPool } from 'models/FilterContextPool';
import { IFilterData } from 'models/Filters';
import ServiceFactory from 'services/ServiceFactory';
import { ActionResult, IApplicationState } from 'store/store';
import { selectContextDataByName, selectCurrentContextData } from './selectors';

import {
  applyFiltersAction,
  IFilterContextData,
  initActionTypes,
  initContextAction,
  manageFiltersAction,
  manageFiltersTypes,
  suggestFiltersAction,
  suggestFiltersActionTypes,
} from './types';

export function suggestFilters(
  searchString: string
): ActionResult<void, suggestFiltersAction> {
  return async (dispatch, getState) => {
    dispatch(action(suggestFiltersActionTypes.SUGGEST_FILTERS_REQUEST));
    const svc = ServiceFactory.getSearchAndFiltersService();
    const currentCtx = selectCurrentContextData(getState());
    if (svc != null && currentCtx !== undefined) {
      await svc.searchFilterSuggestions(searchString, currentCtx).then(res => {
        dispatch(action(suggestFiltersActionTypes.SUGGEST_FILTERS_RESULT, res));
      });
    }
  };
}

export function applyFilters(
  ctxName: string,
  filters: IFilterData[]
): ActionResult<void, applyFiltersAction> {
  return async dispatch => {
    const ctx = FilterContextPool.getContextByName(ctxName);
    ctx.onApplyFilters(filters, dispatch);
  };
}

export function search(
  ctxName: string,
  searchStr: string
): ActionResult<void, applyFiltersAction> {
  return async dispatch => {
    const ctx = FilterContextPool.getContextByName(ctxName);
    ctx.onSearch(searchStr, dispatch);
  };
}

export function initContexts(): ActionResult<void, initContextAction> {
  return async dispatch => {
    dispatch(
      action(
        initActionTypes.REGISTER_CONTEXT_SUCCESS,
        FilterContextPool.initContextsData()
      )
    );
  };
}

export function changeContext(
  ctxName?: string
): ActionResult<void, initContextAction | suggestFiltersAction> {
  return async (dispatch, getState) => {
    dispatch(action(initActionTypes.CHANGE_CONTEXT, ctxName));
    dispatch(action(suggestFiltersActionTypes.SUGGEST_FILTERS_RESULT, []));
    if (ctxName !== undefined && FilterContextPool.hasContext(ctxName)) {
      const ctxData = FilterContextPool.getContextByName(ctxName);
      ctxData.onApplyFilters(
        selectContextDataByName(getState(), ctxName).appliedFilters,
        dispatch
      );
    }
  };
}

function saveFilters(ctxName: string, state: IApplicationState) {
  const filters = selectContextDataByName(state, ctxName).appliedFilters;
  localStorage.setItem(`${ctxName}_filter`, JSON.stringify(filters));
}
export function addFilter(
  filter: IFilterData,
  ctxName: string
): ActionResult<void, manageFiltersAction> {
  return async (dispatch, getState) => {
    dispatch(action(manageFiltersTypes.ADD_FILTER, { filter, ctx: ctxName }));
    saveFilters(ctxName, getState());
  };
}

export function editFilter(
  index: number,
  filter: IFilterData,
  ctxName: string
): ActionResult<void, manageFiltersAction> {
  return async (dispatch, getState) => {
    dispatch(
      action(manageFiltersTypes.EDIT_FILTER, { index, filter, ctx: ctxName })
    );
    saveFilters(ctxName, getState());
  };
}

export function removeFilter(
  filter: IFilterData,
  ctxName: string
): ActionResult<void, manageFiltersAction> {
  return async (dispatch, getState) => {
    dispatch(
      action(manageFiltersTypes.REMOVE_FILTER, { filter, ctx: ctxName })
    );
    saveFilters(ctxName, getState());
  };
}
