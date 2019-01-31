import { IFilterData } from '../../components/FilterSelect/FilterSelect';
import { IMetaData, MetaData } from '../../models/IMetaData';

export interface IFilterState {
  appliedFilters: IFilterData[];
  foundFilters: IFilterData[];
  context: string;
  isFiltersSupporting: boolean;
}

export enum filtersActionTypes {
  APPLY_MODELS_FILTER_REQUEST = '@@filters/APPLY_MODELS_FILTER_REQUEST',
  APPLY_MODELS_FILTER_SUCCESS = '@@filters/APPLY_MODELS_FILTER_SUCCESS',
  APPLY_MODELS_FILTER_FAILURE = '@@filters/APPLY_MODELS_FILTER_FAILURE',
  APPLY_PROJECTS_FILTER_REQUEST = '@@filters/APPLY_PROJECTS_FILTER_REQUEST',
  APPLY_PROJECTS_FILTER_SUCCESS = '@@filters/APPLY_PROJECTS_FILTER_SUCCESS',
  APPLY_PROJECTS_FILTER_FAILURE = '@@filters/APPLY_PROJECTS_FILTER_FAILURE',
  SEARCH_FILTERS_REQUEST = '@@filters/SEARCH_FILTERS_REQUEST',
  SEARCH_FILTERS_RESULT = '@@filters/SEARCH_FILTERS_RESULT',
  CHANGE_CONTEXT = '@@filters/CHANGE_CONTEXT',
  SET_METADATA = '@@filters/SET_METADATA',
  IS_FILTERS_SUPPORT = '@@filters/IS_FILTERS_SUPPORT'
}

export type initContextAction =
  | { type: filtersActionTypes.CHANGE_CONTEXT; payload: string }
  | { type: filtersActionTypes.SET_METADATA; payload: MetaData[] }
  | { type: filtersActionTypes.IS_FILTERS_SUPPORT; payload: boolean };

export type searchFiltersAction =
  | { type: filtersActionTypes.SEARCH_FILTERS_REQUEST }
  | { type: filtersActionTypes.SEARCH_FILTERS_RESULT; payload: IFilterData[] };

export type applyModelsFilterAction =
  | { type: filtersActionTypes.APPLY_MODELS_FILTER_REQUEST }
  | { type: filtersActionTypes.APPLY_MODELS_FILTER_SUCCESS; payload: IFilterData[] | undefined }
  | { type: filtersActionTypes.APPLY_MODELS_FILTER_FAILURE };

export type applyProjectsFilterAction =
  | { type: filtersActionTypes.APPLY_PROJECTS_FILTER_REQUEST }
  | { type: filtersActionTypes.APPLY_PROJECTS_FILTER_SUCCESS; payload: IFilterData[] | undefined }
  | { type: filtersActionTypes.APPLY_PROJECTS_FILTER_FAILURE };
