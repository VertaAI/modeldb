import { action } from 'typesafe-actions';

import { FilterContextPool } from 'models/FilterContextPool';
import { IFilterData } from 'models/Filters';
import ServiceFactory from 'services/ServiceFactory';
import { ActionResult, IApplicationState } from 'store/store';
import { selectContextDataByName, selectCurrentContextData } from './selectors';

import {
  changeContextActionTypes,
  IChangeContextAction,
  IManageFiltersAction,
  IRegisterContextActions,
  ISuggestFiltersActions,
  manageFiltersTypes,
  registerContextActionTypes,
  suggestFiltersActionTypes,
} from './types';

export function suggestFilters(
  searchString: string
): ActionResult<void, ISuggestFiltersActions> {
  return async (dispatch, getState) => {
    dispatch(action(suggestFiltersActionTypes.request));
    const svc = ServiceFactory.getSearchAndFiltersService();
    const currentCtx = selectCurrentContextData(getState());
    if (svc != null && currentCtx !== undefined) {
      await svc.searchFilterSuggestions(searchString, currentCtx).then(res => {
        dispatch(action(suggestFiltersActionTypes.success, res));
      });
    }
  };
}

export function applyFilters(
  ctxName: string,
  filters: IFilterData[]
): ActionResult<void> {
  return async dispatch => {
    const ctx = FilterContextPool.getContextByName(ctxName);
    ctx.onApplyFilters(filters, dispatch);
  };
}

export function search(ctxName: string, searchStr: string): ActionResult<void> {
  return async dispatch => {
    const ctx = FilterContextPool.getContextByName(ctxName);
    ctx.onSearch(searchStr, dispatch);
  };
}

export function initContexts(): ActionResult<void, IRegisterContextActions> {
  return async dispatch => {
    dispatch(
      action(
        registerContextActionTypes.success,
        FilterContextPool.initContextsData()
      )
    );
  };
}

export function changeContext(
  ctxName?: string
): ActionResult<void, IChangeContextAction | ISuggestFiltersActions> {
  return async (dispatch, getState) => {
    dispatch(action(changeContextActionTypes.CHANGE_CONTEXT, ctxName));
    dispatch(action(suggestFiltersActionTypes.success, []));
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
): ActionResult<void, IManageFiltersAction> {
  return async (dispatch, getState) => {
    dispatch(action(manageFiltersTypes.ADD_FILTER, { filter, ctx: ctxName }));
    saveFilters(ctxName, getState());
  };
}

export function editFilter(
  index: number,
  filter: IFilterData,
  ctxName: string
): ActionResult<void, IManageFiltersAction> {
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
): ActionResult<void, IManageFiltersAction> {
  return async (dispatch, getState) => {
    dispatch(
      action(manageFiltersTypes.REMOVE_FILTER, { filter, ctx: ctxName })
    );
    saveFilters(ctxName, getState());
  };
}
