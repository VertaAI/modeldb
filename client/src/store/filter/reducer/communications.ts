import { combineReducers } from 'redux';

import { makeCommunicationReducerFromEnum } from 'utils/redux/communication';

import {
  applyFiltersActionTypes,
  IFilterState,
  registerContextActionTypes,
  searchActionTypes,
  suggestFiltersActionTypes,
} from '../types';

export default combineReducers<IFilterState['communications']>({
  suggestingFilters: makeCommunicationReducerFromEnum(
    suggestFiltersActionTypes
  ),
  searching: makeCommunicationReducerFromEnum(searchActionTypes),
  applyingFilters: makeCommunicationReducerFromEnum(applyFiltersActionTypes),
  registeringContext: makeCommunicationReducerFromEnum(
    registerContextActionTypes
  ),
});
