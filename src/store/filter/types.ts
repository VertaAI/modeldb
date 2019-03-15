import { IFilterData } from 'models/Filters';
import { IMetaData } from 'models/IMetaData';

export interface IFilterContextData {
  appliedFilters: IFilterData[];
  metadata: IMetaData[];
  isFiltersSupporting: boolean;
  ctx: string;
}
export interface IFilterState {
  contexts: { [index: string]: IFilterContextData };
  foundFilters?: IFilterData[];
  context?: string;
}

export enum initActionTypes {
  REGISTER_CONTEXT_REQUEST = '@@filters/REGISTER_CONTEXT_REQUEST',
  REGISTER_CONTEXT_SUCCESS = '@@filters/REGISTER_CONTEXT_SUCCESS',
  REGISTER_CONTEXT_FAILURE = '@@filters/REGISTER_CONTEXT_FAILURE',
  CHANGE_CONTEXT = '@@filters/CHANGE_CONTEXT'
}

export enum suggestFiltersActionTypes {
  SUGGEST_FILTERS_REQUEST = '@@filters/SUGGEST_FILTERS_REQUEST',
  SUGGEST_FILTERS_RESULT = '@@filters/SUGGEST_FILTERS_RESULT'
}

export enum searchActionType {
  SEARCH_REQUEST = '@@filters/SEARCH_REQUEST',
  SEARCH_SUCCESS = '@@filters/SEARCH_SUCCESS',
  SEARCH_FAILURE = '@@filters/SEARCH_FAILURE'
}

export enum applyFiltersActionType {
  APPLY_FILTERS_REQUEST = '@@filters/APPLY_FILTERS_REQUEST',
  APPLY_FILTERS_SUCCESS = '@@filters/APPLY_FILTERS_SUCCESS',
  APPLY_FILTERS_FAILURE = '@@filters/APPLY_FILTERS_FAILURE'
}

export enum manageFiltersTypes {
  ADD_FILTER = '@@filters/ADD_FILTER',
  EDIT_FILTER = '@@filters/EDIT_FILTER',
  REMOVE_FILTER = '@@filters/REMOVE_FILTER'
}

interface IFilterPayload {
  index?: number;
  filter: IFilterData;
  ctx: string;
}
export type initContextAction =
  | { type: initActionTypes.CHANGE_CONTEXT; payload: string }
  | { type: initActionTypes.REGISTER_CONTEXT_REQUEST }
  | { type: initActionTypes.REGISTER_CONTEXT_SUCCESS; payload: IFilterContextData[] }
  | { type: initActionTypes.REGISTER_CONTEXT_FAILURE };

export type applyFiltersAction =
  | { type: applyFiltersActionType.APPLY_FILTERS_REQUEST; payload: IFilterData[] }
  | { type: applyFiltersActionType.APPLY_FILTERS_SUCCESS }
  | { type: applyFiltersActionType.APPLY_FILTERS_FAILURE };

export type suggestFiltersAction =
  | { type: suggestFiltersActionTypes.SUGGEST_FILTERS_REQUEST }
  | { type: suggestFiltersActionTypes.SUGGEST_FILTERS_RESULT; payload: IFilterData[] };

export type searchAction =
  | { type: searchActionType.SEARCH_REQUEST; payload: string }
  | { type: searchActionType.SEARCH_SUCCESS }
  | { type: searchActionType.SEARCH_FAILURE };

export type manageFiltersAction =
  | { type: manageFiltersTypes.EDIT_FILTER; payload: IFilterPayload }
  | { type: manageFiltersTypes.ADD_FILTER; payload: IFilterPayload }
  | { type: manageFiltersTypes.REMOVE_FILTER; payload: IFilterPayload };
