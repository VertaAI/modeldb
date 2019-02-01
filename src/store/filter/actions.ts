import { async } from 'q';
import { ActionResult } from 'store/store';
import { action } from 'typesafe-actions';
import { IFilterData } from '../../components/FilterSelect/FilterSelect';
import { MetaData } from '../../models/IMetaData';
import ServiceFactory from '../../services/ServiceFactory';
import {
  filtersActionTypes,
  IFilterContextData,
  initContextAction,
  manageFiltersAction,
  manageFiltersTypes,
  searchFiltersAction
} from './types';

export function searchFilters<T extends MetaData>(searchString: string): ActionResult<void, searchFiltersAction> {
  return async (dispatch, getState) => {
    dispatch(action(filtersActionTypes.SEARCH_FILTERS_REQUEST));
    const svc = ServiceFactory.getSearchAndFiltersService<T>(getState().filters.context);
    if (svc != null) {
      await svc.searchFilters(searchString).then(res => {
        dispatch(action(filtersActionTypes.SEARCH_FILTERS_RESULT, res));
      });
    }
  };
}
export function initContext<T extends MetaData>(ctx: string, data: IFilterContextData): ActionResult<void, initContextAction> {
  return async (dispatch, getState) => {
    dispatch(action(filtersActionTypes.REGISTER_CONTEXT, data));
    dispatch(action(filtersActionTypes.CHANGE_CONTEXT, ctx));
    const svc = ServiceFactory.getSearchAndFiltersService<T>(ctx);
    if (svc != null) {
      svc.setMetaData(data.metadata);
    }
  };
}

export function resetContext(): ActionResult<void, initContextAction | searchFiltersAction> {
  return async (dispatch, getState) => {
    dispatch(action(filtersActionTypes.CHANGE_CONTEXT, ''));
    dispatch(action(filtersActionTypes.SEARCH_FILTERS_RESULT, []));
  };
}

export function addFilter(filter: IFilterData): ActionResult<void, manageFiltersAction> {
  return async (dispatch, getState) => {
    dispatch(action(manageFiltersTypes.ADD_FILTER, filter));
  };
}

export function editFilter(filter: IFilterData): ActionResult<void, manageFiltersAction> {
  return async (dispatch, getState) => {
    dispatch(action(manageFiltersTypes.EDIT_FILTER, filter));
  };
}

export function removeFilter(filter: IFilterData): ActionResult<void, manageFiltersAction> {
  return async (dispatch, getState) => {
    dispatch(action(manageFiltersTypes.REMOVE_FILTER, filter));
  };
}
