import { History } from 'history';
import { ThunkDispatch } from 'redux-thunk';
import { action } from 'typesafe-actions';

import { IFilterData, IIdFilterData } from 'core/features/filter/Model';

import {
  hasContext,
  selectContextDataByName,
  selectCurrentContextName,
  selectCurrentContextAppliedFilters,
  selectContextFilters,
} from './selectors';
import {
  changeContextActionTypes,
  IChangeContextAction,
  IFilterContext,
  IFilterContextData,
  IManageFiltersAction,
  IResetCurrentContextAction,
  manageFiltersTypes,
  resetCurrentContextActionType,
  IRegisterContextAction,
  registerContextActionType,
  ActionResult,
  IFilterRootState,
} from './types';

export function applyFilters(
  ctxName: string,
  filters: IFilterData[]
): ActionResult<void> {
  return async (dispatch, getState) => {
    const ctx = selectContextDataByName(getState(), ctxName).ctx;
    ctx.onApplyFilters(filters, dispatch);
  };
}

export function applyCurrentContextFilters(): ActionResult<void> {
  return async (dispatch, getState) => {
    const appliedFilters = selectCurrentContextAppliedFilters(getState());
    dispatch(
      applyFilters(selectCurrentContextName(getState())!, appliedFilters)
    );
  };
}

export function setContext(
  context: IFilterContext
): ActionResult<void, IRegisterContextAction> {
  return async (dispatch, getState) => {
    dispatch(registerContext(context));
    dispatch(changeContext(context.name));
  };
}

function registerContext(
  ctx: IFilterContext
): ActionResult<void, IRegisterContextAction> {
  return async (dispatch, _, deps) => {
    const filters = (() => {
      const filtersFromUrl = getFiltersFromUrl();
      const filtersFromLocalStorage = getFiltersFromLocalStorage(ctx.name);
      if (filtersFromUrl.length > 0) {
        return filtersFromUrl;
      }
      return filtersFromLocalStorage;
    })();
    const resultItem: IFilterContextData = {
      ctx,
      filters,
      name: ctx.name,
    };

    if (filters.length !== 0) {
      saveFilters(deps.history, ctx.name, filters);
    }

    dispatch(action(registerContextActionType.REGISTER_CONTEXT, resultItem));
  };
}

function changeContext(
  ctxName: string
): ActionResult<void, IChangeContextAction> {
  return async (dispatch, getState) => {
    dispatch(action(changeContextActionTypes.CHANGE_CONTEXT, ctxName));
    if (hasContext(getState(), ctxName)) {
      const ctx = selectContextDataByName(getState(), ctxName).ctx;
      ctx.onApplyFilters(
        selectCurrentContextAppliedFilters(getState()),
        dispatch
      );
    }
  };
}

// todo refactor
function handleCurrentContextFiltersChanging(
  history: History,
  dispatch: ThunkDispatch<any, any, any>,
  state: IFilterRootState,
  ctxName: string,
  needApplyFilters: boolean,
  needSaveFilters: boolean
) {
  if (needSaveFilters) {
    const filters = selectCurrentContextAppliedFilters(state);
    saveFilters(history, ctxName, filters);
  }
  if (needApplyFilters) {
    dispatch(applyCurrentContextFilters());
  }
}

export function addFilterToCurrentContext(
  filter: IFilterData
): ActionResult<void, IManageFiltersAction> {
  return async (dispatch, getState, deps) => {
    const ctx = selectCurrentContextName(getState())!;
    dispatch(action(manageFiltersTypes.ADD_FILTER, { filter, ctx }));
    handleCurrentContextFiltersChanging(
      deps.history,
      dispatch,
      getState(),
      ctx,
      isNeedApplyFilter(filter),
      isNeedSaveFilter(filter)
    );
  };
}

export function editFilterInCurrentContext(
  filter: IFilterData
): ActionResult<void, IManageFiltersAction> {
  return async (dispatch, getState, deps) => {
    const ctx = selectCurrentContextName(getState())!;
    dispatch(action(manageFiltersTypes.EDIT_FILTER, { filter, ctx }));
    handleCurrentContextFiltersChanging(
      deps.history,
      dispatch,
      getState(),
      ctx,
      isNeedApplyFilter(filter),
      isNeedSaveFilter(filter)
    );
  };
}

export function removeFilterFromCurrentContext(
  filter: IFilterData
): ActionResult<void, IManageFiltersAction> {
  return async (dispatch, getState, deps) => {
    const ctx = selectCurrentContextName(getState())!;
    dispatch(action(manageFiltersTypes.REMOVE_FILTER, { filter, ctx }));
    handleCurrentContextFiltersChanging(
      deps.history,
      dispatch,
      getState(),
      ctx,
      true,
      true
    );
  };
}

export function resetCurrentContextFilters(
  isApplyFilters: boolean
): ActionResult<void, IManageFiltersAction> {
  return async (dispatch, getState, deps) => {
    const ctx = selectCurrentContextName(getState())!;
    dispatch(action(manageFiltersTypes.RESET_FILTERS, { ctx }));
    handleCurrentContextFiltersChanging(
      deps.history,
      dispatch,
      getState(),
      ctx,
      isApplyFilters,
      true
    );
  };
}

function isEditedFilter(filter: IFilterData) {
  return 'isEdited' in filter ? filter.isEdited : false;
}
function isNeedApplyFilter(filter: IFilterData) {
  return 'isEdited' in filter ? !filter.isEdited : true;
}
function isNeedSaveFilter(filter: IFilterData) {
  return 'isEdited' in filter ? !filter.isEdited : true;
}

export function resetCurrentContext(): IResetCurrentContextAction {
  return { type: resetCurrentContextActionType.RESET_CURRENT_CONTEXT };
}

export function saveEditedFilterInCurrentContext(
  filter: IIdFilterData
): ActionResult<void, IManageFiltersAction> {
  return async (dispatch, getState, deps) => {
    if (filter.value.length === 0) {
      removeFilterFromCurrentContext({ ...filter, isEdited: false })(
        dispatch,
        getState,
        deps
      );
    } else {
      editFilterInCurrentContext({ ...filter, isEdited: false })(
        dispatch,
        getState,
        deps
      );
    }
  };
}

export function updateContextFilters(
  ctxName: string,
  update: (filters: IFilterData[]) => IFilterData[]
): ActionResult<void, IManageFiltersAction> {
  return async (dispatch, getState) => {
    if (hasContext(getState(), ctxName)) {
      const contextFilters = selectContextFilters(getState(), ctxName);
      const updatedContextFilters = update(contextFilters);
      saveFiltersInLocalStorage(ctxName, updatedContextFilters);
      dispatch(
        action(manageFiltersTypes.UPDATE_FILTERS, {
          ctx: ctxName,
          filters: updatedContextFilters,
        })
      );
    } else {
      const contextFilters = getFiltersFromLocalStorage(ctxName);
      const updatedContextFilters = update(contextFilters);
      saveFiltersInLocalStorage(ctxName, updatedContextFilters);
      dispatch(
        action(manageFiltersTypes.UPDATE_FILTER_IN_LOCAL_STORAGE, {
          ctx: ctxName,
          filters: updatedContextFilters,
        })
      );
    }
  };
}

function saveFilters(
  history: History,
  ctxName: string,
  filters: IFilterData[]
) {
  saveFiltersInUrl(history, filters);
  saveFiltersInLocalStorage(ctxName, filters);
}

const URLFiltersParam = 'filters';
function saveFiltersInUrl(history: History, filters: IFilterData[]) {
  const urlParams = new URLSearchParams(window.location.search);
  if (filters.length === 0) {
    urlParams.delete(URLFiltersParam);
  } else {
    urlParams.set(URLFiltersParam, encodeURIComponent(JSON.stringify(filters)));
  }
  history.push({
    search: String(urlParams),
  });
}
function getFiltersFromUrl(): IFilterData[] {
  const filtersFromUrl = new URLSearchParams(window.location.search).get(
    URLFiltersParam
  );
  return filtersFromUrl ? JSON.parse(decodeURIComponent(filtersFromUrl)) : [];
}

const makeLocalStorageFiltersKey = (ctxName: string) => `${ctxName}_filter`;
function saveFiltersInLocalStorage(ctxName: string, filters: IFilterData[]) {
  const localStorageKey = ctxName.endsWith('_filter')
    ? ctxName
    : makeLocalStorageFiltersKey(ctxName);
  // todo refactor it
  if (filters.length === 0) {
    localStorage.removeItem(localStorageKey);
  } else {
    localStorage.setItem(localStorageKey, JSON.stringify(filters));
  }
}
function getFiltersFromLocalStorage(ctxName: string) {
  const confData = localStorage.getItem(makeLocalStorageFiltersKey(ctxName));
  if (confData) {
    return JSON.parse(confData);
  }
  return [];
}
