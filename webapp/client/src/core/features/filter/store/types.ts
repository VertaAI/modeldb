import { History } from 'history';
import { AnyAction, Action } from 'redux';
import { ThunkDispatch, ThunkAction } from 'redux-thunk';

import {
  IFilterData,
  IQuickFilter,
  PropertyType,
} from 'core/features/filter/Model';
import {
  MakeCommunicationActions,
  makeCommunicationActionTypes,
} from 'core/shared/utils/redux/communication';

export interface IFilterRootState {
  filters: IFilterState;
}

export interface IThunkActionDependencies {
  history: History;
}

export type ActionResult<R = void, A extends Action = AnyAction> = ThunkAction<
  R,
  IFilterRootState,
  IThunkActionDependencies,
  A
>;

export interface IFilterState {
  data: {
    contexts: { [contextName: string]: IFilterContextData };
    currentContextName?: string;
  };
}

export interface IFilterContextData {
  filters: IFilterData[];
  ctx: IFilterContext;
  name: string;
}

export interface IFilterContext {
  name: string;
  quickFilters: IQuickFilter[];
  onApplyFilters(
    filters: IFilterData[],
    dispatch: ThunkDispatch<any, any, AnyAction>
  ): void;
}

export enum registerContextActionType {
  REGISTER_CONTEXT = '@@filters/REGISTER_CONTEXT',
}
export interface IRegisterContextAction {
  type: registerContextActionType.REGISTER_CONTEXT;
  payload: IFilterContextData;
}

export enum changeContextActionTypes {
  CHANGE_CONTEXT = '@@filters/CHANGE_CONTEXT',
}
export interface IChangeContextAction {
  type: changeContextActionTypes.CHANGE_CONTEXT;
  payload: string | undefined;
}

export const applyFiltersActionTypes = makeCommunicationActionTypes({
  REQUEST: '@@filters/APPLY_FILTERS_REQUEST',
  SUCCESS: '@@filters/APPLY_FILTERS_SUCСESS',
  FAILURE: '@@filters/APPLY_FILTERS_FAILURE',
});
export type IApplyFiltersActions = MakeCommunicationActions<
  typeof applyFiltersActionTypes,
  { request: IFilterData[] }
>;

export enum manageFiltersTypes {
  ADD_FILTER = '@@filters/ADD_FILTER',
  EDIT_FILTER = '@@filters/EDIT_FILTER',
  REMOVE_FILTER = '@@filters/REMOVE_FILTER',
  UPDATE_FILTERS = '@@filters/UPDATE_FILTERS',
  UPDATE_FILTER_IN_LOCAL_STORAGE = '@@filters/UPDATE_FILTER_IN_LOCAL_STORAGE',
  RESET_FILTERS = '@@filters/RESET_FILTERS',
}
export interface IFilterPayload {
  filter: IFilterData;
  ctx: string;
}
export type IManageFiltersAction =
  | { type: manageFiltersTypes.EDIT_FILTER; payload: IFilterPayload }
  | { type: manageFiltersTypes.ADD_FILTER; payload: IFilterPayload }
  | { type: manageFiltersTypes.REMOVE_FILTER; payload: IFilterPayload }
  | {
      type: manageFiltersTypes.UPDATE_FILTERS;
      payload: { ctx: string; filters: IFilterData[] };
    }
  | {
      type: manageFiltersTypes.UPDATE_FILTER_IN_LOCAL_STORAGE;
      payload: { ctx: string; filters: IFilterData[] };
    }
  | {
      type: manageFiltersTypes.RESET_FILTERS;
      payload: { ctx: string };
    };

export enum resetCurrentContextActionType {
  RESET_CURRENT_CONTEXT = '@@filters/RESET_CURRENT_CONTEXT',
}
export interface IResetCurrentContextAction {
  type: resetCurrentContextActionType.RESET_CURRENT_CONTEXT;
}

export enum applyEditedFilterActionType {
  APPLY_EDITED_FILTER = '@@filters/APPLY_EDITED_FILTER',
}
export interface IApplyEditedFilter {
  type: applyEditedFilterActionType.APPLY_EDITED_FILTER;
  payload: { type: PropertyType };
}

export type FeatureAction =
  | IManageFiltersAction
  | IApplyFiltersActions
  | IRegisterContextAction
  | IChangeContextAction
  | IResetCurrentContextAction
  | IApplyEditedFilter;
