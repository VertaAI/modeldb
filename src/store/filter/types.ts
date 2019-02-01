import { IFilterData } from '../../components/FilterSelect/FilterSelect';
import { IMetaData, MetaData } from '../../models/IMetaData';
import { HashMap } from '../../types/HashMap';

export interface IFilterContextData {
  appliedFilters: IFilterData[];
  metadata: IMetaData[];
  isFiltersSupporting: boolean;
  ctx: string;
}
export interface IFilterState {
  contexts: HashMap<IFilterContextData>;
  // currentContext?: IFilterContextData;
  foundFilters?: IFilterData[];
  context?: string;
}

export enum filtersActionTypes {
  SEARCH_FILTERS_REQUEST = '@@filters/SEARCH_FILTERS_REQUEST',
  SEARCH_FILTERS_RESULT = '@@filters/SEARCH_FILTERS_RESULT',
  CHANGE_CONTEXT = '@@filters/CHANGE_CONTEXT',
  REGISTER_CONTEXT = '@@filters/REGISTER_CONTEXT'
}

export enum applyFiltersActionType {
  SEARCH_REQUEST = '@@filters/SEARCH_REQUEST',
  SEARCH_SUCCESS = '@@filters/SEARCH_SUCCESS',
  SEARCH_FAILURE = '@@filters/SEARCH_FAILURE'
}

export enum manageFiltersTypes {
  ADD_FILTER = '@@filters/ADD_FILTER',
  EDIT_FILTER = '@@filters/EDIT_FILTER',
  REMOVE_FILTER = '@@filters/REMOVE_FILTER'
}

export type initContextAction =
  | { type: filtersActionTypes.CHANGE_CONTEXT; payload: string }
  | { type: filtersActionTypes.REGISTER_CONTEXT; payload: IFilterContextData };

export type searchFiltersAction =
  | { type: filtersActionTypes.SEARCH_FILTERS_REQUEST }
  | { type: filtersActionTypes.SEARCH_FILTERS_RESULT; payload: IFilterData[] };

export type manageFiltersAction =
  | { type: manageFiltersTypes.ADD_FILTER; payload: IFilterData }
  | { type: manageFiltersTypes.EDIT_FILTER; payload: IFilterData }
  | { type: manageFiltersTypes.REMOVE_FILTER; payload: IFilterData };

export type applyFiltersAction =
  | { type: applyFiltersActionType.SEARCH_REQUEST; payload: IFilterData[] }
  | { type: filtersActionTypes.REGISTER_CONTEXT; payload: IFilterContextData };
