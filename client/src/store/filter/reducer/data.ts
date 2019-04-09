import { AnyAction, Reducer } from 'redux';

import { IFilterData } from 'models/Filters';
import composeReducers from 'utils/redux/composeReducers';
import {
  changeContextActionTypes,
  FeatureAction,
  IFilterContextData,
  IFilterState,
  IManageFiltersAction,
  manageFiltersTypes,
  registerContextActionTypes,
  suggestFiltersActionTypes,
} from '../types';

const initial: IFilterState['data'] = {
  contexts: {},
};

function checkManageFiltersAction(
  action: AnyAction
): action is IManageFiltersAction {
  return Object.values(manageFiltersTypes).includes(action.type);
}

const manageFiltersReducer: Reducer<IFilterState['data'], FeatureAction> = (
  state = initial,
  action
) => {
  if (checkManageFiltersAction(action)) {
    const ctx = action.payload.ctx;
    if (ctx === undefined || state.contexts[ctx] === undefined) {
      throw new Error('Current filters context is undefined');
    }

    const newState = { ...state };
    switch (action.type) {
      case manageFiltersTypes.ADD_FILTER: {
        const ctx = action.payload.ctx;
        const currentCtx: IFilterContextData | undefined =
          newState.contexts[ctx];
        if (currentCtx !== undefined) {
          currentCtx.appliedFilters = [
            ...currentCtx.appliedFilters,
            action.payload.filter,
          ];
          newState.contexts[ctx] = currentCtx;
        }
        break;
      }
      case manageFiltersTypes.REMOVE_FILTER: {
        const currentCtx: IFilterContextData | undefined =
          newState.contexts[ctx];
        if (currentCtx !== undefined) {
          currentCtx.appliedFilters = [...currentCtx.appliedFilters];
          currentCtx.appliedFilters.splice(
            currentCtx.appliedFilters.indexOf(action.payload.filter),
            1
          );
          newState.contexts[ctx] = currentCtx;
        }
        break;
      }
      case manageFiltersTypes.EDIT_FILTER: {
        const ctxData = newState.contexts[ctx];
        if (ctx !== undefined && action.payload.index !== undefined) {
          const newAppliedFilters = Array<IFilterData>();

          newAppliedFilters.push(
            ...ctxData.appliedFilters.slice(0, action.payload.index - 1),
            action.payload.filter,
            ...ctxData.appliedFilters.slice(action.payload.index + 1)
          );
          ctxData.appliedFilters = newAppliedFilters;
          newState.contexts[ctx] = ctxData;
        }
        break;
      }
      default: {
        return state;
      }
    }
    return newState;
  }
  return state;
};

const commonReducer: Reducer<IFilterState['data'], FeatureAction> = (
  state = initial,
  action
) => {
  switch (action.type) {
    case registerContextActionTypes.SUCCESS: {
      const data: IFilterContextData[] = action.payload;
      const newMap: { [index: string]: IFilterContextData } = {};
      for (const ctxData of data) {
        newMap[ctxData.name] = ctxData;
      }

      return { ...state, contexts: newMap };
    }
    case changeContextActionTypes.CHANGE_CONTEXT: {
      return { ...state, context: action.payload };
      // const contexts = state.contexts;
      // if (contexts[action.payload] !== undefined) {
      //   return { ...state, context: action.payload };
      // }
      // return { ...state, context: undefined };
    }
    case suggestFiltersActionTypes.SUCCESS: {
      return { ...state, foundFilters: action.payload };
    }
    default:
      return state;
  }
};

export default composeReducers([commonReducer, manageFiltersReducer]);
