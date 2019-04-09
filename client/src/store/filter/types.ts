import { IFilterContext } from 'models/FilterContextPool';
import { IFilterData } from 'models/Filters';
import {
  ICommunication,
  makeCommunicationActionTypes,
  MakeCommunicationActions,
} from 'utils/redux/communication';

export interface IFilterState {
  data: {
    contexts: { [index: string]: IFilterContextData };
    foundFilters?: IFilterData[];
    context?: string;
  };
  communications: {
    suggestingFilters: ICommunication;
    searching: ICommunication;
    applyingFilters: ICommunication;
    registeringContext: ICommunication;
  };
}

export const registerContextActionTypes = makeCommunicationActionTypes({
  request: '@@filters/REGISTER_CONTEXT_REQUEST',
  success: '@@filters/REGISTER_CONTEXT_SUCСESS',
  failure: '@@filters/REGISTER_CONTEXT_FAILURE',
});
export interface IFilterContextData {
  appliedFilters: IFilterData[];
  ctx: IFilterContext;
  name: string;
}
export type IRegisterContextActions = MakeCommunicationActions<
  typeof registerContextActionTypes,
  { success: IFilterContextData[] }
>;

export enum changeContextActionTypes {
  CHANGE_CONTEXT = '@@filters/CHANGE_CONTEXT',
}
export interface IChangeContextAction {
  type: changeContextActionTypes.CHANGE_CONTEXT;
  payload: string;
}

export const suggestFiltersActionTypes = makeCommunicationActionTypes({
  request: '@@filters/SUGGEST_FILTERS_REQUEST',
  success: '@@filters/SUGGEST_FILTERS_SUCСESS',
  failure: '@@filters/SUGGEST_FILTERS_FAILURE',
});
export type ISuggestFiltersActions = MakeCommunicationActions<
  typeof suggestFiltersActionTypes,
  { success: IFilterData[] }
>;

export const searchActionTypes = makeCommunicationActionTypes({
  request: '@@filters/SEARCH_REQUEST',
  success: '@@filters/SEARCH_SUCСESS',
  failure: '@@filters/SEARCH_FAILURE',
});
export type ISearchActions = MakeCommunicationActions<
  typeof searchActionTypes,
  { request: string }
>;

export const applyFiltersActionTypes = makeCommunicationActionTypes({
  request: '@@filters/APPLY_FILTERS_REQUEST',
  success: '@@filters/APPLY_FILTERS_SUCСESS',
  failure: '@@filters/APPLY_FILTERS_FAILURE',
});
export type IApplyFiltersActions = MakeCommunicationActions<
  typeof applyFiltersActionTypes,
  { request: IFilterData[] }
>;

export enum manageFiltersTypes {
  ADD_FILTER = '@@filters/ADD_FILTER',
  EDIT_FILTER = '@@filters/EDIT_FILTER',
  REMOVE_FILTER = '@@filters/REMOVE_FILTER',
}
export interface IFilterPayload {
  index?: number;
  filter: IFilterData;
  ctx: string;
}
export type IManageFiltersAction =
  | { type: manageFiltersTypes.EDIT_FILTER; payload: IFilterPayload }
  | { type: manageFiltersTypes.ADD_FILTER; payload: IFilterPayload }
  | { type: manageFiltersTypes.REMOVE_FILTER; payload: IFilterPayload };

export type FeatureAction =
  | ISearchActions
  | IManageFiltersAction
  | IApplyFiltersActions
  | IRegisterContextActions
  | IChangeContextAction
  | ISuggestFiltersActions;
