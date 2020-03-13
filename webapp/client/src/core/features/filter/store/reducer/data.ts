import * as R from 'ramda';
import { Reducer } from 'redux';

import { IFilterData } from 'core/features/filter/Model';
import { updateById } from 'core/shared/utils/collection';
import composeReducers from 'core/shared/utils/redux/composeReducers';

import {
  changeContextActionTypes,
  FeatureAction,
  IFilterState,
  manageFiltersTypes,
  resetCurrentContextActionType,
  registerContextActionType,
} from '../types';

const initial: IFilterState['data'] = {
  contexts: {},
};

const adjustContextFilters = (
  f: (v: IFilterData[]) => IFilterData[],
  contextName: string,
  state: IFilterState['data']
): IFilterState['data'] => {
  if (!state.contexts[contextName]) {
    throw new Error('Current filters context is undefined');
  }
  return {
    ...state,
    contexts: {
      ...state.contexts,
      [contextName]: {
        ...state.contexts[contextName],
        filters: f(state.contexts[contextName].filters),
      },
    },
  };
};

const manageFiltersReducer: Reducer<IFilterState['data'], FeatureAction> = (
  state = initial,
  action
) => {
  switch (action.type) {
    case manageFiltersTypes.ADD_FILTER: {
      return adjustContextFilters(
        filters => filters.concat(action.payload.filter),
        action.payload.ctx,
        state
      );
    }
    case manageFiltersTypes.REMOVE_FILTER: {
      return adjustContextFilters(
        filters =>
          R.reject(filter => filter.id === action.payload.filter.id, filters),
        action.payload.ctx,
        state
      );
    }
    case manageFiltersTypes.EDIT_FILTER: {
      return adjustContextFilters(
        filters =>
          updateById(
            _ => action.payload.filter,
            action.payload.filter.id,
            filters
          ),
        action.payload.ctx,
        state
      );
    }
    case manageFiltersTypes.UPDATE_FILTERS: {
      return adjustContextFilters(
        _ => action.payload.filters,
        action.payload.ctx,
        state
      );
    }
    case manageFiltersTypes.RESET_FILTERS: {
      return adjustContextFilters(_ => [], action.payload.ctx, state);
    }
    default: {
      return state;
    }
  }
};

const commonReducer: Reducer<IFilterState['data'], FeatureAction> = (
  state = initial,
  action
) => {
  switch (action.type) {
    case registerContextActionType.REGISTER_CONTEXT: {
      return {
        ...state,
        contexts: {
          ...state.contexts,
          [action.payload.name]: action.payload,
        },
      };
    }
    case resetCurrentContextActionType.RESET_CURRENT_CONTEXT: {
      return {
        ...state,
        currentContextName: undefined,
      };
    }
    case changeContextActionTypes.CHANGE_CONTEXT: {
      return { ...state, currentContextName: action.payload };
    }
    default:
      return state;
  }
};

export { initial };
export default composeReducers([commonReducer, manageFiltersReducer]);
