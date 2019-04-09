import { combineReducers } from 'redux';

import { makeCommunicationReducerFromEnum } from 'utils/redux/communication';

import {
  IFilterState,
  suggestFiltersActionTypes,
  searchActionTypes,
  applyFiltersActionTypes,
  registerContextActionTypes,
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
